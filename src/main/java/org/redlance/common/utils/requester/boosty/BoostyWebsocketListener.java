package org.redlance.common.utils.requester.boosty;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.kosmx.emotes.server.config.Serializer;
import org.apache.commons.io.input.CharSequenceReader;
import org.redlance.common.CommonUtils;
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
import java.util.function.Consumer;

public class BoostyWebsocketListener implements WebSocket.Listener  {
    private static final URI ENDPOINT = URI.create("wss://pubsub.boosty.to/connection/websocket");

    private final Map<Integer, CompletableFuture<JsonElement>> messages = new ConcurrentHashMap<>();

    private final String token;
    private final int userId;

    private final HttpClient httpClient;
    private final WebSocket.Builder webSocketBuilder;

    protected WebSocket webSocket;

    protected Consumer<JsonObject> listener;

    public BoostyWebsocketListener(String token, int userId) {
        this.token = token;
        this.userId = userId;

        this.httpClient = HttpClient.newBuilder()
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build();

        this.webSocketBuilder = this.httpClient.newWebSocketBuilder()
                .header("Origin", "https://boosty.to");
    }

    public CompletableFuture<Void> connect(final Consumer<JsonObject> listener) {
        return this.webSocketBuilder.buildAsync(ENDPOINT, this).thenAccept((e) -> {
            this.webSocket = e;

            sendMessage(new OutboundAuthMessage.Auth("js", this.token))
                    .whenCompleteAsync((
                            (element, throwable) ->  {
                                sendMessage(new OutboundAuthMessage.Subscribe("users#" + this.userId))
                                        .join();

                                sendMessage(new OutboundAuthMessage.Subscribe("dialogs#" + this.userId))
                                        .join();
                            }
                    )).whenCompleteAsync((element1, throwable1) -> {
                        this.listener = listener;
                        this.messages.clear();
                    }).join();
        }).exceptionally((ex) -> {
            CommonUtils.LOGGER.error("Failed to connect!", ex);
            return null;
        });
    }

    public CompletableFuture<JsonElement> sendMessage(Object params) {
        int id = this.messages.size() + 1;

        CompletableFuture<JsonElement> future = this.messages.compute(id, (k, v) -> new CompletableFuture<>());

        this.webSocket.sendText(new Gson().toJson( // Boosty don't accept pretty gson
                new OutboundAuthMessage(id, id > 1 ? 1 : 0, Serializer.serializer.toJsonTree(params))
        ), true);

        return future;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        CommonUtils.LOGGER.warn("Disconnected from boosty {}!", reason);

        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        CommonUtils.LOGGER.error("WebSocket exception!", error);

        WebSocket.Listener.super.onError(webSocket, error);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        CommonUtils.LOGGER.trace("Received message: {}", data);

        try (Reader reader = new CharSequenceReader(data)) {
            if (this.listener != null) {
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
            } else {
                InboundAuthMessage message = Serializer.serializer.fromJson(reader, InboundAuthMessage.class);

                CompletableFuture<JsonElement> future = this.messages.get(message.id());
                if (future == null) {
                    CommonUtils.LOGGER.warn("Unknown message {}: {}!", message.id(), message.result());
                    return WebSocket.Listener.super.onText(webSocket, data, last);
                }

                future.complete(message.result());
            }
        } catch (Throwable th) {
            CommonUtils.LOGGER.error("Failed to handle!", th);
        }

        return WebSocket.Listener.super.onText(webSocket, data, last);
    }
}
