package org.redlance.common.utils.requester.boosty;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.kosmx.emotes.server.config.Serializer;
import org.apache.commons.io.input.CharSequenceReader;
import org.jetbrains.annotations.Nullable;
import org.redlance.common.CommonUtils;
import org.redlance.common.utils.requester.Requester;
import org.redlance.common.utils.requester.boosty.handler.BoostyBuiltInHandler;
import org.redlance.common.utils.requester.boosty.messages.auth.InboundAuthMessage;
import org.redlance.common.utils.requester.boosty.messages.auth.OutboundAuthMessage;
import org.redlance.common.utils.requester.boosty.messages.generic.InboundChannelMessage;

import java.io.BufferedReader;
import java.net.URI;
import java.net.http.WebSocket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class BoostyWebsocketListener implements WebSocket.Listener  {
    private static final URI ENDPOINT = URI.create("wss://pubsub.boosty.to/connection/websocket");

    public static final List<String> KNOWN_CHANNEL_PREFIXES = List.of(
            "users#", "dialogs#", "blogger:"//, "$dialog:"
    );

    private final Map<Integer, CompletableFuture<JsonObject>> messages = new ConcurrentHashMap<>();

    private final BiConsumer<String, JsonObject> listener;
    private final Supplier<String> token;
    private final int userId;

    private final WebSocket.Builder webSocketBuilder;

    protected ScheduledFuture<?> pinger;
    protected WebSocket webSocket;

    /**
     * @param listener Event listener (For example: {@link BoostyBuiltInHandler}).
     * @param token Boosty access token.
     * @param userId Boosty shelf user id.
     */
    public BoostyWebsocketListener(BiConsumer<String, JsonObject> listener, Supplier<String> token, int userId) {
        this.listener = listener;
        this.token = token;
        this.userId = userId;

        this.webSocketBuilder = Requester.HTTP_CLIENT.newWebSocketBuilder()
                .header("Origin", "https://boosty.to");
    }

    /**
     * Connecting to websocket.
     */
    public CompletableFuture<Void> connect() {
        if (this.pinger != null) {
            this.pinger.cancel(true);
            this.pinger = null;
        }

        return this.webSocketBuilder.buildAsync(ENDPOINT, this).thenAccept((e) -> {
            this.webSocket = e;
            this.messages.clear();

            JsonObject responseObj = sendMessage(0, new OutboundAuthMessage.Auth("js", this.token.get()))
                    .whenCompleteAsync(((element, throwable) ->  {
                        for (String channel : KNOWN_CHANNEL_PREFIXES) {
                            sendMessage(1, new OutboundAuthMessage.Subscribe(channel + this.userId))
                                    .join();
                        }
                    }))
                    .join();

            this.pinger = CommonUtils.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                    () -> sendMessage(7, null), 30L, 30L, TimeUnit.SECONDS
            );

            CommonUtils.LOGGER.info("Connected to boosty! (Backend version: {})",
                    responseObj.has("version") ? responseObj.get("version").getAsString() : responseObj
            );
        }).exceptionally((ex) -> {
            CommonUtils.LOGGER.error("Failed to connect!", ex);
            return null;
        });
    }

    /**
     * @param method 0 - default, 1 - channel subscribe, 2 - channel unsubscribe, 7 - ping
     * @param params Payload, null for ping.
     */
    public CompletableFuture<JsonObject> sendMessage(int method, @Nullable Object params) {
        int id = this.messages.size() + 1;

        CompletableFuture<JsonObject> future = this.messages.compute(id, (k, v) -> new CompletableFuture<>());

        this.webSocket.sendText(new Gson().toJson( // Boosty don't accept pretty gson
                new OutboundAuthMessage(id, method, params == null ? null : Serializer.serializer.toJsonTree(params))
        ), true);

        return future;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        CommonUtils.LOGGER.warn("Disconnected from boosty: {} ({})", reason, statusCode);
        connect();

        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        CommonUtils.LOGGER.error("WebSocket exception!", error);
        connect(); // for connection reset

        WebSocket.Listener.super.onError(webSocket, error);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        try (BufferedReader reader = new BufferedReader(new CharSequenceReader(data))) {
            for (String line : reader.lines().toList()) {
                InboundAuthMessage authMessage = Serializer.serializer.fromJson(line, InboundAuthMessage.class);

                if (authMessage.id() > 0) { // replied
                    CompletableFuture<JsonObject> future = this.messages.get(authMessage.id());
                    if (future == null) {
                        CommonUtils.LOGGER.warn("Unknown message {}: {}!", authMessage.id(), authMessage.result());
                        return WebSocket.Listener.super.onText(webSocket, data, last);
                    }

                    if (authMessage.error() != null) {
                        future.completeExceptionally(new RuntimeException(
                                authMessage.error().toString()
                        ));
                    } else {
                        future.complete(authMessage.result());
                    }
                } else if (authMessage.result() != null) {
                    InboundChannelMessage message = Serializer.serializer.fromJson(authMessage.result(),
                            InboundChannelMessage.class);

                    if (message.data() == null || !message.data().has("data")) {
                        CommonUtils.LOGGER.debug("Invalid object in channel {}! ({})", message.channel(), message.data());
                        return WebSocket.Listener.super.onText(webSocket, data, last);
                    }

                    this.listener.accept(message.channel(), message.data());
                }
            }
        } catch (Throwable th) {
            CommonUtils.LOGGER.error("Failed to handle {}!", data, th);
        }

        return WebSocket.Listener.super.onText(webSocket, data, last);
    }
}
