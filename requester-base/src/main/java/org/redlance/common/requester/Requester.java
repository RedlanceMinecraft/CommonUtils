package org.redlance.common.requester;

import com.github.mizosoft.methanol.*;
import com.github.mizosoft.methanol.internal.Utils;
import org.redlance.common.requester.interceptors.CacheOverrideInterceptor;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * @deprecated A client shared by every requester hands all of them the needs of one, like
 * {@link CacheOverrideInterceptor}. Build a client of your own from {@link HttpClients#newBuilder(String)}: it shares
 * the cache and the other settings.
 */
@Deprecated
@SuppressWarnings("unused")
public class Requester {
    /**
     * @deprecated Use {@link HttpClients#APPLICATION_JACKSON_SMILE}.
     */
    @Deprecated
    public static final MediaType APPLICATION_JACKSON_SMILE = HttpClients.APPLICATION_JACKSON_SMILE;

    /**
     * The shared settings with the {@link HttpClients#CACHE} and {@link CacheOverrideInterceptor}.
     *
     * @deprecated Build a client of your own from {@link HttpClients#newBuilder(String)}.
     */
    @Deprecated
    public static final Methanol HTTP_CLIENT = HttpClients.newBuilder("http-requester")
            .cache(HttpClients.CACHE)
            .backendInterceptor(new CacheOverrideInterceptor())
            .build();

    /**
     * @deprecated Use {@code client.send(httpRequest, type).body()}.
     */
    @Deprecated
    public static <T> T sendRequest(HttpRequest httpRequest, Class<T> type) throws IOException, InterruptedException {
        return Requester.HTTP_CLIENT // send request
                .send(httpRequest, type)
                .body();
    }

    /**
     * @deprecated Use {@code client.send(httpRequest, token).body()}.
     */
    @Deprecated
    public static <T> T sendRequest(HttpRequest httpRequest, TypeRef<T> token) throws IOException, InterruptedException {
        return Requester.HTTP_CLIENT // send request
                .send(httpRequest, token)
                .body();
    }

    /**
     * @deprecated Use {@code client.send(httpRequest, bodyHandler).body()}.
     */
    @Deprecated
    public static <T> T sendRequest(HttpRequest httpRequest, HttpResponse.BodyHandler<T> bodyHandler) throws IOException, InterruptedException {
        return Requester.HTTP_CLIENT // send request
                .send(httpRequest, bodyHandler)
                .body();
    }

    /**
     * @deprecated Use {@code client.sendAsync(httpRequest, type).thenApply(HttpResponse::body)}.
     */
    @Deprecated
    public static <T> CompletableFuture<T> sendRequestAsync(HttpRequest httpRequest, Class<T> type) {
        return Requester.HTTP_CLIENT // send request
                .sendAsync(httpRequest, type)
                .thenApply(HttpResponse::body);
    }

    /**
     * @deprecated Use {@code client.sendAsync(httpRequest, token).thenApply(HttpResponse::body)}.
     */
    @Deprecated
    public static <T> CompletableFuture<T> sendRequestAsync(HttpRequest httpRequest, TypeRef<T> token) {
        return Requester.HTTP_CLIENT // send request
                .sendAsync(httpRequest, token)
                .thenApply(HttpResponse::body);
    }

    /**
     * @deprecated Use {@code client.sendAsync(httpRequest, bodyHandler).thenApply(HttpResponse::body)}.
     */
    @Deprecated
    public static <T> CompletableFuture<T> sendRequestAsync(HttpRequest httpRequest, HttpResponse.BodyHandler<T> bodyHandler) {
        return Requester.HTTP_CLIENT // send request
                .sendAsync(httpRequest, bodyHandler)
                .thenApply(HttpResponse::body);
    }

    /**
     * @deprecated Use {@code client.send(httpRequest, HttpResponse.BodyHandlers.discarding())}.
     */
    @Deprecated
    public static HttpResponse<Void> sendRequestVoid(HttpRequest httpRequest) throws IOException, InterruptedException {
        return Requester.HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.discarding());
    }

    /**
     * @deprecated Use {@code client.sendAsync(httpRequest, HttpResponse.BodyHandlers.discarding())}.
     */
    @Deprecated
    public static void sendRequestVoidAsync(HttpRequest httpRequest) {
        Requester.HTTP_CLIENT.sendAsync(httpRequest, HttpResponse.BodyHandlers.discarding());
    }

    /**
     * @deprecated Use {@code HttpClients.CACHE.remove(httpRequest)}.
     */
    @Deprecated
    public static boolean invalidateRequest(HttpRequest httpRequest) {
        try {
            Optional<HttpCache> httpCache = Requester.HTTP_CLIENT.cache();

            if (httpCache.isEmpty()) {
                return false;
            }

            return httpCache.get().remove(httpRequest);
        } catch (Throwable th) {
            RequesterUtils.LOGGER.warn("Failed to remove request from cache!", th);
            return false;
        }
    }

    /**
     * @deprecated Let the client encode the object: {@link MutableRequest#POST(Object, MediaType)}.
     */
    @Deprecated
    public static HttpRequest.BodyPublisher ofObject(Object object, MediaType mediaType) {
        return HTTP_CLIENT.adapterCodec().orElseThrow().publisherOf(object, Utils.hintsOf(mediaType));
    }
}
