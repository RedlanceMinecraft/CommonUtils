package org.redlance.common.utils.requester.boosty;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.kosmx.emotes.server.config.Serializer;
import org.apache.commons.io.input.CharSequenceReader;
import org.jetbrains.annotations.Nullable;
import org.redlance.common.CommonUtils;
import org.redlance.common.utils.requester.boosty.handler.BoostyBuiltInHandler;
import org.redlance.common.utils.requester.boosty.messages.auth.InboundAuthMessage;
import org.redlance.common.utils.requester.boosty.messages.auth.OutboundAuthMessage;
import org.redlance.common.utils.requester.boosty.messages.generic.InboundChannelMessage;

import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class BoostyWebsocketListener implements WebSocket.Listener  {
    private static final URI ENDPOINT = URI.create("wss://pubsub.boosty.to/connection/websocket");

    private final Map<Integer, CompletableFuture<JsonObject>> messages = new ConcurrentHashMap<>();

    private final Consumer<JsonObject> listener;
    private final String token;
    private final int userId;

    private final HttpClient httpClient;
    private final WebSocket.Builder webSocketBuilder;

    protected ScheduledFuture<?> pinger;
    protected WebSocket webSocket;

    /**
     * @param listener Event listener (For example: {@link BoostyBuiltInHandler}).
     * @param token Boosty access token.
     * @param userId Boosty shelf user id.
     */
    public BoostyWebsocketListener(Consumer<JsonObject> listener, String token, int userId) {
        this.listener = listener;
        this.token = token;
        this.userId = userId;

        this.httpClient = HttpClient.newBuilder()
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build();

        this.webSocketBuilder = this.httpClient.newWebSocketBuilder()
                .header("Origin", "https://boosty.to");
    }

    /**
     * Connecting to websocket.
     */
    public CompletableFuture<Void> connect() {
        return this.webSocketBuilder.buildAsync(ENDPOINT, this).thenAccept((e) -> {
            this.webSocket = e;

            sendMessage(0, new OutboundAuthMessage.Auth("js", this.token))
                    .whenCompleteAsync((
                            (element, throwable) ->  {
                                sendMessage(1, new OutboundAuthMessage.Subscribe("users#" + this.userId))
                                        .join();

                                sendMessage(1, new OutboundAuthMessage.Subscribe("dialogs#" + this.userId))
                                        .join();
                            }
                    )).join();

            this.pinger = CommonUtils.EXECUTOR.scheduleAtFixedRate(
                    () -> sendMessage(7, null), 30L, 30L, TimeUnit.SECONDS
            );
        }).exceptionally((ex) -> {
            CommonUtils.LOGGER.error("Failed to connect!", ex);
            return null;
        });
    }

    /**
     * @param method 0 - default, 1 - channel subscribe, 7 - ping
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
        CommonUtils.LOGGER.warn("Disconnected from boosty {}!", reason);

        this.pinger.cancel(true);
        this.messages.clear();
        this.connect();

        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        CommonUtils.LOGGER.error("WebSocket exception!", error);

        WebSocket.Listener.super.onError(webSocket, error);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        try (Reader reader = new CharSequenceReader(data)) {
            JsonObject jsonObject = Serializer.serializer.fromJson(reader, JsonObject.class);

            if (jsonObject.has("id")) { // replied
                InboundAuthMessage message = Serializer.serializer.fromJson(reader, InboundAuthMessage.class);

                CompletableFuture<JsonObject> future = this.messages.get(message.id());
                if (future == null) {
                    CommonUtils.LOGGER.warn("Unknown message {}: {}!", message.id(), message.result());
                    return WebSocket.Listener.super.onText(webSocket, data, last);
                }

                future.complete(message.result());

            } else if (this.listener != null) {
                InboundChannelMessage message = Serializer.serializer.fromJson(
                        Serializer.serializer.fromJson(reader, JsonObject.class)
                                .getAsJsonObject("result"),
                        InboundChannelMessage.class
                );

                if (message.data() == null || !message.data().has("data")) {
                    CommonUtils.LOGGER.warn("Boosty new session??? {}", message.data());
                    return WebSocket.Listener.super.onText(webSocket, data, last);
                }

                this.listener.accept(message.data());
            }
        } catch (Throwable th) {
            CommonUtils.LOGGER.error("Failed to handle!", th);
        }

        return WebSocket.Listener.super.onText(webSocket, data, last);
    }
}
