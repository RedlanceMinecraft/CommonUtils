package org.redlance.common.requester.boosty;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redlance.common.jackson.JacksonMappers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.IntStream;

class BoostyWebsocketListenerTest {
    private static final String PUBLICATION = """
            {"result": {"channel": "users#42", "data": {"data": {"type": "subscriber_new", "data": {}}}}}""";

    // Pings only where a test asks for them, so that they do not mix into what the others count
    private static final BoostyWebsocketListener.Timing TIMING = new BoostyWebsocketListener.Timing(
            Duration.ofMillis(300), Duration.ofHours(1), Duration.ofMillis(100)
    );
    private static final BoostyWebsocketListener.Timing PINGING = new BoostyWebsocketListener.Timing(
            Duration.ofMillis(200), Duration.ofMillis(100), Duration.ofMillis(100)
    );

    private final List<Map.Entry<String, ObjectNode>> events = new CopyOnWriteArrayList<>();
    private final List<Boolean> connects = new CopyOnWriteArrayList<>();
    private final List<Throwable> disconnects = new CopyOnWriteArrayList<>();
    private final List<String> disconnectThreads = new CopyOnWriteArrayList<>();
    private final BlockingQueue<FakeWebSocket> opened = new LinkedBlockingQueue<>();
    private final AtomicInteger tokenFailures = new AtomicInteger();
    private BoostyWebsocketListener listener;

    @AfterEach
    void closeListener() {
        if (this.listener != null) {
            this.listener.close();
        }
    }

    @Test
    void connectsAndSubscribes() throws Exception {
        listener(TIMING).start();
        FakeWebSocket socket = nextSocket();

        String auth = socket.awaitSent(1).getFirst();
        JsonNode authJson = JacksonMappers.OBJECT_MAPPER.readTree(auth);
        Assertions.assertEquals(0, authJson.path("method").asInt(0));
        Assertions.assertEquals("socket-token", authJson.path("params").path("token").asString());

        Thread.sleep(50);
        Assertions.assertEquals(1, socket.sent.size(), "Nothing goes out before the authorization");

        socket.receive("{\"id\": " + FakeWebSocket.id(auth) + ", \"result\": {\"version\": \"5.0\"}}");

        List<String> subscriptions = socket.awaitSent(4).subList(1, 4);
        Assertions.assertEquals(List.of("users#42", "dialogs#42", "blogger:42"), subscriptions.stream()
                .map(text -> JacksonMappers.OBJECT_MAPPER.readTree(text).path("params").path("channel").asString())
                .toList());
        Assertions.assertFalse(this.listener.isConnected(), "Not up before it subscribed");

        subscriptions.forEach(text -> socket.receive("{\"id\": " + FakeWebSocket.id(text) + "}"));
        await(() -> this.listener.isConnected());
        Assertions.assertEquals(List.of(false), this.connects);
    }

    @Test
    void startsOnce() throws Exception {
        BoostyWebsocketListener listener = listener(TIMING);
        listener.start();
        listener.start();

        nextSocket();
        Assertions.assertNull(this.opened.poll(300, TimeUnit.MILLISECONDS));
    }

    @Test
    void sendsOneCommandAtATime() throws Exception {
        FakeWebSocket socket = connected();
        int handshake = socket.sent.size();

        List<CompletableFuture<ObjectNode>> replies = IntStream.range(0, 50).parallel()
                .mapToObj(i -> this.listener.sendMessage(7, null))
                .toList();

        List<Integer> ids = socket.awaitSent(handshake + 50).subList(handshake, handshake + 50).stream()
                .map(FakeWebSocket::id)
                .toList();
        Assertions.assertEquals(50, Set.copyOf(ids).size(), "Every command has its own id");

        ids.forEach(id -> socket.receive("{\"id\": " + id + "}"));
        for (CompletableFuture<ObjectNode> reply : replies) {
            Assertions.assertNull(reply.get(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void failedSendFailsCommand() throws Exception {
        FakeWebSocket socket = connected();
        socket.failSends = true;

        ExecutionException exception = Assertions.assertThrows(ExecutionException.class,
                () -> this.listener.sendMessage(7, null).get(5, TimeUnit.SECONDS));
        Assertions.assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    void failsWithoutConnection() {
        ExecutionException exception = Assertions.assertThrows(ExecutionException.class,
                () -> listener(TIMING).sendMessage(7, null).get(5, TimeUnit.SECONDS));
        Assertions.assertInstanceOf(IllegalStateException.class, exception.getCause());
    }

    @Test
    void retriesFailedAuthorization() throws Exception {
        listener(TIMING).start();
        FakeWebSocket first = nextSocket();

        String auth = first.awaitSent(1).getFirst();
        first.receive("{\"id\": " + FakeWebSocket.id(auth) + ", \"error\": {\"code\": 109, \"message\": \"token expired\"}}");

        FakeWebSocket second = nextSocket();
        Assertions.assertTrue(first.aborted, "A connection that cannot subscribe is closed");
        Assertions.assertEquals(1, first.sent.size(), "No subscriptions without the authorization");

        handshake(second);
        await(() -> this.listener.isConnected());
        Assertions.assertEquals(List.of(false), this.connects);
    }

    @Test
    void retriesUnansweredAuthorization() throws Exception {
        listener(TIMING).start();
        FakeWebSocket first = nextSocket();
        first.awaitSent(1);

        FakeWebSocket second = nextSocket();
        Assertions.assertTrue(first.aborted);

        handshake(second);
        await(() -> this.listener.isConnected());
    }

    @Test
    void retriesWhenTokenFails() throws Exception {
        this.tokenFailures.set(1);
        listener(TIMING).start();

        FakeWebSocket socket = nextSocket();
        Assertions.assertEquals(-1, this.tokenFailures.get(), "The socket opens only once a token came");

        handshake(socket);
        await(() -> this.listener.isConnected());
        Assertions.assertFalse(socket.aborted);
    }

    @Test
    void closeWhileOpeningAbortsSocket() throws Exception {
        CompletableFuture<WebSocket> opening = new CompletableFuture<>();
        BlockingQueue<WebSocket.Listener> connections = new LinkedBlockingQueue<>();
        listener(TIMING, connection -> {
            connections.add(connection);
            return opening;
        }).start();

        WebSocket.Listener connection = connections.poll(5, TimeUnit.SECONDS);
        Assertions.assertNotNull(connection, "No connection was opened");
        this.listener.close();

        FakeWebSocket socket = new FakeWebSocket(connection);
        connection.onOpen(socket);
        opening.complete(socket);

        Assertions.assertTrue(socket.aborted, "A socket that opens after close() is aborted");
        Assertions.assertTrue(socket.sent.isEmpty());
    }

    @Test
    void reconnectsAfterDrop() throws Exception {
        FakeWebSocket first = connected();

        first.listener.onClose(first, 1006, "gone");
        Assertions.assertFalse(this.listener.isConnected());
        Assertions.assertEquals(1, this.disconnects.size());

        handshake(nextSocket());
        await(() -> this.listener.isConnected());
        Assertions.assertEquals(List.of(false, true), this.connects, "The new connection tells that events were missed");
    }

    @Test
    void reconnectsWhenPingGoesUnanswered() throws Exception {
        FakeWebSocket first = connected(PINGING);

        first.awaitSent(5); // the handshake and a ping
        FakeWebSocket second = nextSocket();

        Assertions.assertTrue(first.aborted, "A connection that does not answer is dropped");
        Assertions.assertEquals(1, this.disconnects.size());
        Assertions.assertTrue(this.disconnectThreads.getFirst().startsWith("boosty-websocket-"),
                "The timeout runs on the listener's scheduler, not on the JDK's: " + this.disconnectThreads);
        handshake(second);
        await(() -> this.listener.isConnected());
    }

    @Test
    void keepsConnectionThatAnswersPings() throws Exception {
        FakeWebSocket socket = connected(PINGING);
        socket.answerPings = true;

        Thread.sleep(700);

        Assertions.assertTrue(socket.sent.size() > 5, "Pinged");
        Assertions.assertTrue(this.listener.isConnected());
        Assertions.assertEquals(List.of(), this.disconnects);
        Assertions.assertNull(this.opened.poll());
    }

    @Test
    void deliversEventsOnlyFromConnectionThatIsUp() throws Exception {
        FakeWebSocket first = connected();
        first.receive(PUBLICATION);
        Assertions.assertEquals(1, this.events.size());

        first.listener.onError(first, new IOException("reset"));
        first.receive(PUBLICATION);
        Assertions.assertEquals(1, this.events.size(), "Lost");

        FakeWebSocket second = nextSocket();
        second.awaitSent(1);
        second.receive(PUBLICATION);
        Assertions.assertEquals(1, this.events.size(), "Not up yet");

        handshake(second);
        await(() -> this.listener.isConnected());
        second.receive(PUBLICATION);
        Assertions.assertEquals(2, this.events.size());
        Assertions.assertEquals("users#42", this.events.getLast().getKey());
    }

    @Test
    void joinsMessageParts() throws Exception {
        FakeWebSocket socket = connected();
        int sent = socket.sent.size();
        CompletableFuture<ObjectNode> reply = this.listener.sendMessage(0, null);
        int id = FakeWebSocket.id(socket.awaitSent(sent + 1).get(sent));

        socket.receive("{\"id\": " + id + ", \"result\": {\"vers", false);
        Assertions.assertFalse(reply.isDone());
        socket.receive("ion\": \"2.0\"}}", true);

        Assertions.assertEquals("2.0", reply.get(5, TimeUnit.SECONDS).get("version").asString());
    }

    @Test
    void handlesEveryLine() throws Exception {
        FakeWebSocket socket = connected();
        int sent = socket.sent.size();
        CompletableFuture<ObjectNode> reply = this.listener.sendMessage(7, null);
        int id = FakeWebSocket.id(socket.awaitSent(sent + 1).get(sent));

        socket.receive(String.join("\n",
                "{\"id\": 1000000}",
                "not json",
                "{\"id\": " + id + "}",
                PUBLICATION
        ));

        Assertions.assertNull(reply.get(5, TimeUnit.SECONDS));
        Assertions.assertEquals(1, this.events.size());
        Assertions.assertEquals("subscriber_new",
                this.events.getFirst().getValue().path("data").path("type").asString());
    }

    @Test
    void dropFailsUnansweredCommands() throws Exception {
        FakeWebSocket socket = connected();
        CompletableFuture<ObjectNode> unanswered = this.listener.sendMessage(7, null);

        socket.listener.onError(socket, new IOException("reset"));

        ExecutionException exception = Assertions.assertThrows(ExecutionException.class,
                () -> unanswered.get(5, TimeUnit.SECONDS));
        Assertions.assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    void closeStopsEverything() throws Exception {
        FakeWebSocket socket = connected();

        this.listener.close();

        Assertions.assertTrue(socket.aborted);
        Assertions.assertFalse(this.listener.isConnected());
        Assertions.assertThrows(ExecutionException.class,
                () -> this.listener.sendMessage(7, null).get(5, TimeUnit.SECONDS));

        this.listener.start();
        Assertions.assertNull(this.opened.poll(300, TimeUnit.MILLISECONDS), "No connecting once closed");
        Assertions.assertEquals(List.of(), this.disconnects, "Closing is no loss");
    }

    private BoostyWebsocketListener listener(BoostyWebsocketListener.Timing timing) {
        return listener(timing, connection -> {
            FakeWebSocket socket = new FakeWebSocket(connection);
            connection.onOpen(socket);
            this.opened.add(socket);
            return CompletableFuture.completedFuture(socket);
        });
    }

    private BoostyWebsocketListener listener(BoostyWebsocketListener.Timing timing,
                                             Function<WebSocket.Listener, CompletableFuture<WebSocket>> opener) {
        this.listener = new BoostyWebsocketListener(this::token, 42, new BoostyWebsocketListener.Listener() {
            @Override
            public void onEvent(String channel, ObjectNode event) {
                events.add(Map.entry(channel, event));
            }

            @Override
            public void onConnected(boolean reconnected) {
                connects.add(reconnected);
            }

            @Override
            public void onDisconnected(Throwable cause) {
                disconnects.add(cause);
                disconnectThreads.add(Thread.currentThread().getName());
            }
        }, opener, timing);

        return this.listener;
    }

    private String token() {
        if (this.tokenFailures.getAndDecrement() > 0) {
            throw new IllegalStateException("No token");
        }

        return "socket-token";
    }

    private FakeWebSocket connected() throws Exception {
        return connected(TIMING);
    }

    private FakeWebSocket connected(BoostyWebsocketListener.Timing timing) throws Exception {
        listener(timing).start();
        FakeWebSocket socket = nextSocket();
        handshake(socket);
        await(() -> this.listener.isConnected());
        return socket;
    }

    private FakeWebSocket nextSocket() throws InterruptedException {
        FakeWebSocket socket = this.opened.poll(5, TimeUnit.SECONDS);
        Assertions.assertNotNull(socket, "No connection was opened");
        return socket;
    }

    /**
     * Answers the authorization and the subscriptions.
     */
    private static void handshake(FakeWebSocket socket) throws InterruptedException {
        String auth = socket.awaitSent(1).getFirst();
        socket.receive("{\"id\": " + FakeWebSocket.id(auth) + ", \"result\": {\"version\": \"5.0\"}}");

        for (String subscription : socket.awaitSent(4).subList(1, 4)) {
            socket.receive("{\"id\": " + FakeWebSocket.id(subscription) + "}");
        }
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                Assertions.fail("Timed out");
            }
            Thread.sleep(5);
        }
    }

    /**
     * Records what is sent and, like the JDK, fails a send while another one is pending.
     */
    private static final class FakeWebSocket implements WebSocket {
        private final WebSocket.Listener listener;
        private final List<String> sent = new CopyOnWriteArrayList<>();
        private final AtomicBoolean pending = new AtomicBoolean();
        private volatile boolean failSends;
        private volatile boolean answerPings;
        private volatile boolean aborted;

        FakeWebSocket(WebSocket.Listener listener) {
            this.listener = listener;
        }

        static int id(String text) {
            return JacksonMappers.OBJECT_MAPPER.readTree(text).path("id").intValue();
        }

        void receive(String text) {
            receive(text, true);
        }

        void receive(String text, boolean last) {
            this.listener.onText(this, text, last);
        }

        List<String> awaitSent(int count) throws InterruptedException {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (this.sent.size() < count) {
                if (System.nanoTime() > deadline) {
                    Assertions.fail("Sent " + this.sent.size() + " of " + count);
                }
                Thread.sleep(5);
            }
            return List.copyOf(this.sent);
        }

        @Override
        public CompletableFuture<WebSocket> sendText(CharSequence data, boolean last) {
            if (!this.pending.compareAndSet(false, true)) {
                return CompletableFuture.failedFuture(new IllegalStateException("Send pending"));
            }

            if (this.failSends || this.aborted) {
                this.pending.set(false);
                return CompletableFuture.failedFuture(new IOException("Output closed"));
            }

            String text = data.toString();
            this.sent.add(text);

            if (this.answerPings && JacksonMappers.OBJECT_MAPPER.readTree(text).path("method").asInt() == 7) {
                CompletableFuture.delayedExecutor(5, TimeUnit.MILLISECONDS)
                        .execute(() -> receive("{\"id\": " + id(text) + "}"));
            }

            return CompletableFuture.supplyAsync(() -> {
                this.pending.set(false);
                return this;
            }, CompletableFuture.delayedExecutor(2, TimeUnit.MILLISECONDS));
        }

        @Override
        public CompletableFuture<WebSocket> sendBinary(ByteBuffer data, boolean last) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendPing(ByteBuffer message) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendPong(ByteBuffer message) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendClose(int statusCode, String reason) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public void request(long n) {
        }

        @Override
        public String getSubprotocol() {
            return "";
        }

        @Override
        public boolean isOutputClosed() {
            return this.aborted;
        }

        @Override
        public boolean isInputClosed() {
            return this.aborted;
        }

        @Override
        public void abort() {
            this.aborted = true;
        }
    }
}
