package org.redlance.common.requester.boosty;

import org.redlance.common.jackson.JacksonMappers;
import org.redlance.common.requester.RequesterUtils;
import org.redlance.common.requester.boosty.messages.auth.InboundAuthMessage;
import org.redlance.common.requester.boosty.messages.auth.OutboundAuthMessage;
import org.redlance.common.requester.boosty.messages.generic.InboundChannelMessage;
import org.redlance.common.utils.CommonExecutors;
import tools.jackson.databind.node.ObjectNode;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Keeps a connection to the Boosty pubsub websocket from {@link #start()} until {@link #close()}.
 * <p>
 * A connection takes a token from the supplier, then opens the socket, authorizes, subscribes to the
 * {@link #KNOWN_CHANNEL_PREFIXES} channels of the user and pings every 30 seconds. The listener connects again when the
 * connection drops or leaves a ping unanswered for 15 seconds, and every 30 seconds while connecting fails.
 * {@link Listener#onConnected(boolean)} tells when events may have been missed in between.
 */
public final class BoostyWebsocketListener implements AutoCloseable {
    private static final URI ENDPOINT = URI.create("wss://pubsub.boosty.to/connection/websocket");

    private static final Timing TIMING = new Timing(Duration.ofSeconds(15), Duration.ofSeconds(30), Duration.ofSeconds(30));
    private static final ScheduledExecutorService EXECUTOR = CommonExecutors.createScheduledExecutor(1, "boosty-websocket-");
    // The token supplier usually sends a request, which must not hold up the pings
    private static final ExecutorService TOKEN_EXECUTOR = CommonExecutors.createExecutor("boosty-websocket-token-");

    public static final List<String> KNOWN_CHANNEL_PREFIXES = List.of(
            "users#", "dialogs#", "blogger:"//, "$dialog:"
    );

    private final Supplier<String> token;
    private final int userId;
    private final Listener listener;
    private final Function<WebSocket.Listener, CompletableFuture<WebSocket>> opener;
    private final Timing timing;

    private final AtomicInteger lastMessageId = new AtomicInteger();

    // Guards the connections, the retry and the flags
    private final Object stateLock = new Object();
    private volatile @Nullable Connection current;
    private @Nullable Connection pending;
    private @Nullable ScheduledFuture<?> retry;
    private boolean started;
    private boolean everConnected;
    private volatile boolean closed;

    /**
     * @param token Socket token supplier, see {@link BoostyRequester#requestSocketToken(String)}.
     * @param userId Boosty shelf user id.
     * @param listener Receives the events and the state of the connection (For example:
     * {@link org.redlance.common.requester.boosty.handler.BoostyBuiltInHandler}).
     */
    public BoostyWebsocketListener(Supplier<String> token, int userId, Listener listener) {
        this(token, userId, listener, BoostyWebsocketListener::open, TIMING);
    }

    /**
     * @param opener Opens a socket that reports to the given listener.
     */
    BoostyWebsocketListener(Supplier<String> token, int userId, Listener listener,
                            Function<WebSocket.Listener, CompletableFuture<WebSocket>> opener, Timing timing) {
        this.token = token;
        this.userId = userId;
        this.listener = listener;
        this.opener = opener;
        this.timing = timing;
    }

    private static CompletableFuture<WebSocket> open(WebSocket.Listener listener) {
        return BoostyRequester.HTTP_CLIENT.newWebSocketBuilder()
                .header("Origin", "https://boosty.to")
                .connectTimeout(Duration.ofSeconds(30))
                .buildAsync(ENDPOINT, listener);
    }

    /**
     * Starts keeping the connection; does nothing once started or closed.
     */
    public void start() {
        synchronized (this.stateLock) {
            if (this.started || this.closed) {
                return;
            }

            this.started = true;
        }

        connect();
    }

    /**
     * Whether a connection is up: authorized and subscribed.
     */
    public boolean isConnected() {
        return this.current != null;
    }

    /**
     * Sends a command on the connection.
     *
     * @param method 0 - default, 1 - channel subscribe, 2 - channel unsubscribe, 7 - ping
     * @param params Payload, null for ping.
     * @return The reply; fails if nothing is connected, the command was not sent, Boosty answered with an error, or no
     * reply came in 15 seconds.
     */
    public CompletableFuture<ObjectNode> sendMessage(int method, @Nullable Object params) {
        Connection connection = this.current;
        if (connection == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Not connected to boosty"));
        }

        return connection.send(method, params);
    }

    /**
     * Closes the connection and stops connecting.
     */
    @Override
    public void close() {
        Connection current;
        Connection pending;
        synchronized (this.stateLock) {
            if (this.closed) {
                return;
            }

            this.closed = true;
            if (this.retry != null) {
                this.retry.cancel(false);
            }

            current = this.current;
            pending = this.pending;
            this.current = null;
            this.pending = null;
        }

        IOException cause = new IOException("The listener was closed");
        if (current != null) {
            current.close(cause, true);
        }

        if (pending != null) {
            pending.close(cause, true);
        }
    }

    /**
     * Opens a connection, unless one is up or being opened.
     */
    private void connect() {
        Connection connection;
        synchronized (this.stateLock) {
            if (this.closed || this.current != null || this.pending != null) {
                return;
            }

            connection = new Connection();
            this.pending = connection;
        }

        // The token comes first, so that no socket waits for it unauthorized
        CompletableFuture.supplyAsync(this.token, TOKEN_EXECUTOR).thenCompose(token -> {
            if (connection.closed) {
                return CompletableFuture.failedFuture(new IOException("The connection was closed before it opened"));
            }

            return this.opener.apply(connection).thenCompose(socket -> connection.start(socket, token));
        }).whenComplete((reply, throwable) -> {
            if (throwable == null) {
                takeOver(connection, reply);
            } else {
                failed(connection, throwable);
            }
        });
    }

    private void takeOver(Connection connection, @Nullable ObjectNode reply) {
        boolean takenOver = false;
        boolean reconnected = false;
        synchronized (this.stateLock) {
            if (this.pending == connection && !connection.closed) {
                this.pending = null;
                this.current = connection;
                reconnected = this.everConnected;
                this.everConnected = true;
                connection.startPings();
                takenOver = true;
            }
        }

        if (!takenOver) {
            failed(connection, new IOException("The connection was closed while it subscribed"));
            return;
        }

        RequesterUtils.LOGGER.info("Connected to boosty! (Backend version: {})",
                reply != null && reply.has("version") ? reply.get("version").asString() : reply
        );

        try {
            this.listener.onConnected(reconnected);
        } catch (Throwable th) {
            RequesterUtils.LOGGER.error("Listener failed to handle the connection!", th);
        }
    }

    private void failed(Connection connection, Throwable cause) {
        synchronized (this.stateLock) {
            if (this.pending == connection) {
                this.pending = null;
            }
        }

        connection.close(cause, true);
        if (!this.closed) {
            RequesterUtils.LOGGER.warn("Failed to connect to boosty, retrying in {}!", this.timing.retryDelay(), cause);
            retryLater();
        }
    }

    /**
     * Drops the connection and connects again, at once unless it opened just now.
     *
     * @param abort Whether to abort the socket, which is not needed once it closed.
     */
    private void lost(Connection connection, Throwable cause, boolean abort) {
        boolean wasCurrent;
        synchronized (this.stateLock) {
            wasCurrent = this.current == connection;
            if (wasCurrent) {
                this.current = null;
            }
        }

        connection.close(cause, abort);
        if (!wasCurrent) { // Being opened, so its handshake fails and handles it
            return;
        }

        RequesterUtils.LOGGER.warn("Lost the connection to boosty!", cause);
        try {
            this.listener.onDisconnected(cause);
        } catch (Throwable th) {
            RequesterUtils.LOGGER.error("Listener failed to handle the disconnection!", th);
        }

        // A connection that drops as soon as it opens would otherwise be reopened in a loop
        if (connection.uptime().compareTo(this.timing.retryDelay()) >= 0) {
            connect();
        } else {
            retryLater();
        }
    }

    private void retryLater() {
        synchronized (this.stateLock) {
            if (this.closed || this.current != null || (this.retry != null && !this.retry.isDone())) {
                return;
            }

            this.retry = EXECUTOR.schedule(this::connect, this.timing.retryDelay().toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Receives what the websocket brings. The methods run on the threads of the socket and of the listener's scheduler,
     * which every listener shares, so they should return quickly.
     */
    @FunctionalInterface
    public interface Listener {
        /**
         * An event published in a subscribed channel.
         *
         * @param channel Channel name, e.g. {@code users#<user id>}.
         * @param event   Published event: {@code {"data": {"type": ..., "data": {...}}}}.
         */
        void onEvent(String channel, ObjectNode event);

        /**
         * A connection is up: authorized and subscribed.
         *
         * @param reconnected Whether it replaces a lost connection, so that events published in between were missed.
         */
        default void onConnected(boolean reconnected) {
        }

        /**
         * The connection was lost; another one follows.
         */
        default void onDisconnected(Throwable cause) {
        }
    }

    /**
     * @param replyTimeout How long a command waits for its reply before it fails.
     * @param pingInterval How often a connection is pinged.
     * @param retryDelay   How long after a failed connect, or after a connection that dropped as soon as it opened, the
     *                     next connect follows.
     */
    record Timing(Duration replyTimeout, Duration pingInterval, Duration retryDelay) {
    }

    /**
     * One connection with its own commands, sends and partial text.
     */
    private final class Connection implements WebSocket.Listener {
        private final Map<Integer, CompletableFuture<ObjectNode>> commands = new ConcurrentHashMap<>();

        // WebSocket fails a send while another one is pending, so every send waits for the previous one
        private final Object sendLock = new Object();
        private CompletableFuture<?> lastSend = CompletableFuture.completedFuture(null);

        // A text message may arrive in several parts
        private final StringBuilder partialText = new StringBuilder();

        private volatile @Nullable WebSocket socket;
        private volatile @Nullable ScheduledFuture<?> pings;
        private volatile long connectedAt;
        private volatile boolean closed;

        /**
         * Authorizes and subscribes.
         *
         * @param token Socket token.
         * @return The reply to the authorization.
         */
        private CompletableFuture<ObjectNode> start(WebSocket socket, String token) {
            if (!opened(socket)) {
                return CompletableFuture.failedFuture(new IOException("The connection was closed while it opened"));
            }

            CompletableFuture<ObjectNode> authorized = send(0, new OutboundAuthMessage.Auth("js", token));
            return authorized.thenCompose(reply -> CompletableFuture.allOf(KNOWN_CHANNEL_PREFIXES.stream()
                    .map(channel -> send(1, new OutboundAuthMessage.Subscribe(channel + userId)))
                    .toArray(CompletableFuture[]::new)
            ).thenApply(ignored -> reply));
        }

        /**
         * Keeps the socket, or aborts it if the connection was closed before the socket was known, which close() could
         * not abort then.
         *
         * @return Whether the connection is still open.
         */
        private boolean opened(WebSocket socket) {
            this.socket = socket;
            if (this.closed) {
                socket.abort();
                return false;
            }

            return true;
        }

        private void startPings() {
            this.connectedAt = System.nanoTime();

            long interval = timing.pingInterval().toMillis();
            this.pings = EXECUTOR.scheduleAtFixedRate(() -> send(7, null).whenComplete((reply, throwable) -> {
                if (throwable != null) {
                    lost(this, throwable, true);
                }
            }), interval, interval, TimeUnit.MILLISECONDS);
        }

        private Duration uptime() {
            return Duration.ofNanos(System.nanoTime() - this.connectedAt);
        }

        /**
         * @param abort Whether to abort the socket, which is not needed once it closed.
         */
        private void close(Throwable cause, boolean abort) {
            synchronized (this) {
                if (this.closed) {
                    return;
                }

                this.closed = true;
            }

            ScheduledFuture<?> pings = this.pings;
            if (pings != null) {
                pings.cancel(false);
            }

            WebSocket socket = this.socket;
            if (socket != null && abort) {
                socket.abort();
            }

            this.commands.values().forEach(command -> command.completeExceptionally(cause));
        }

        private CompletableFuture<ObjectNode> send(int method, @Nullable Object params) {
            int id = lastMessageId.incrementAndGet();

            CompletableFuture<ObjectNode> command = new CompletableFuture<>();
            this.commands.put(id, command);

            // Not orTimeout(), which would run what follows a timeout on the one thread of every delay in the JVM
            ScheduledFuture<?> timeout = EXECUTOR.schedule(() -> command.completeExceptionally(new TimeoutException(
                    "Boosty did not reply to command " + id + " in " + timing.replyTimeout()
            )), timing.replyTimeout().toMillis(), TimeUnit.MILLISECONDS);
            command.whenComplete((reply, throwable) -> {
                timeout.cancel(false);
                this.commands.remove(id, command);
            });

            WebSocket socket = this.socket;
            if (this.closed || socket == null) {
                command.completeExceptionally(new IOException("The connection is closed"));
                return command;
            }

            try {
                String text = JacksonMappers.OBJECT_MAPPER.writeValueAsString( // Boosty don't accept pretty gson
                        new OutboundAuthMessage(id, method, params)
                );

                synchronized (this.sendLock) {
                    CompletableFuture<WebSocket> send = this.lastSend
                            .handle((ignored, throwable) -> null)
                            .thenCompose(ignored -> socket.sendText(text, true));

                    this.lastSend = send;
                    send.whenComplete((ignored, throwable) -> {
                        if (throwable != null) {
                            command.completeExceptionally(throwable);
                        }
                    });
                }
            } catch (Throwable th) {
                command.completeExceptionally(th);
            }

            return command;
        }

        private void handle(String line) {
            if (line.isBlank()) {
                return;
            }

            try {
                InboundAuthMessage authMessage = JacksonMappers.OBJECT_MAPPER.readValue(line, InboundAuthMessage.class);

                if (authMessage.id() > 0) { // replied
                    CompletableFuture<ObjectNode> command = this.commands.get(authMessage.id());
                    if (command == null) {
                        RequesterUtils.LOGGER.warn("Unknown message {}: {}!", authMessage.id(), authMessage.result());
                    } else if (authMessage.error() != null) {
                        command.completeExceptionally(new RuntimeException(
                                authMessage.error().toString()
                        ));
                    } else {
                        command.complete(authMessage.result());
                    }

                    return;
                }

                // Only the connection that is up delivers events, so that none comes from one being opened or lost
                if (authMessage.result() == null || current != this) {
                    return;
                }

                InboundChannelMessage message = JacksonMappers.OBJECT_MAPPER.convertValue(authMessage.result(),
                        InboundChannelMessage.class);

                if (message.data() == null || !message.data().has("data")) {
                    RequesterUtils.LOGGER.debug("Invalid object in channel {}! ({})", message.channel(), message.data());
                    return;
                }

                listener.onEvent(message.channel(), message.data());
            } catch (Throwable th) {
                RequesterUtils.LOGGER.error("Failed to handle {}!", line, th);
            }
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            if (opened(webSocket)) {
                WebSocket.Listener.super.onOpen(webSocket);
            }
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            String text = null;

            synchronized (this.partialText) {
                this.partialText.append(data);
                if (last) {
                    text = this.partialText.toString();
                    this.partialText.setLength(0);
                }
            }

            if (text != null) {
                text.lines().forEach(this::handle);
            }

            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            lost(this, new IOException("Disconnected from boosty: " + reason + " (" + statusCode + ")"), false);
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            lost(this, error, false);
        }
    }
}
