package org.redlance.common.utils.requester;

import com.github.mizosoft.methanol.*;
import com.github.mizosoft.methanol.adapter.gson.GsonAdapterFactory;
import com.github.mizosoft.methanol.internal.Utils;
import io.github.kosmx.emotes.server.config.Serializer;
import org.redlance.common.CommonUtils;

import java.io.IOException;
import java.net.CookieManager;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

public class Requester {
    public static final Methanol HTTP_CLIENT = Methanol.newBuilder()
            .executor(CommonUtils.EXECUTOR)
            .connectTimeout(Duration.ofMinutes(1))
            .version(HttpClient.Version.HTTP_1_1)
            .proxy(UrlProxySelector.INSTANCE)
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .cache(HttpCache.newBuilder()
                    .executor(CommonUtils.EXECUTOR)
                    .cacheOnMemory(1024 * 1024 * 1024) // 1024 MBs
                    .listener(new HttpCache.Listener() {
                        @Override
                        public void onNetworkUse(HttpRequest request, TrackedResponse<?> cacheResponse) {
                            CommonUtils.LOGGER.debug("Nework used: {}", request);
                        }
                    })
                    .build()
            )
            .interceptor(new CookieFixerInterceptor())
            .backendInterceptor(new CacheOverrideInterceptor())
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36")
            .cookieHandler(new CookieManager())
            .adapterCodec(AdapterCodec.newBuilder()
                    .decoder(GsonAdapterFactory.createDecoder(Serializer.getSerializer()))
                    .encoder(GsonAdapterFactory.createEncoder(Serializer.getSerializer()))
                    .build())
            .build();

    public static <T> T sendRequest(HttpRequest httpRequest, Class<T> type) throws IOException, InterruptedException {
        return Requester.HTTP_CLIENT // send request
                .send(httpRequest, type)
                .body();
    }

    public static <T> T sendRequest(HttpRequest httpRequest, TypeRef<T> token) throws IOException, InterruptedException {
        return Requester.HTTP_CLIENT // send request
                .send(httpRequest, token)
                .body();
    }

    public static <T> T sendRequest(HttpRequest httpRequest, HttpResponse.BodyHandler<T> bodyHandler) throws IOException, InterruptedException {
        return Requester.HTTP_CLIENT // send request
                .send(httpRequest, bodyHandler)
                .body();
    }

    public static <T> CompletableFuture<T> sendRequestAsync(HttpRequest httpRequest, Class<T> type) {
        return Requester.HTTP_CLIENT // send request
                .sendAsync(httpRequest, type)
                .thenApply(HttpResponse::body);
    }

    public static <T> CompletableFuture<T> sendRequestAsync(HttpRequest httpRequest, TypeRef<T> token) {
        return Requester.HTTP_CLIENT // send request
                .sendAsync(httpRequest, token)
                .thenApply(HttpResponse::body);
    }

    public static <T> CompletableFuture<T> sendRequestAsync(HttpRequest httpRequest, HttpResponse.BodyHandler<T> bodyHandler) {
        return Requester.HTTP_CLIENT // send request
                .sendAsync(httpRequest, bodyHandler)
                .thenApply(HttpResponse::body);
    }

    public static void sendRequestVoid(HttpRequest httpRequest) throws IOException, InterruptedException {
        Requester.HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.discarding());
    }

    public static void sendRequestVoidAsync(HttpRequest httpRequest) {
        Requester.HTTP_CLIENT.sendAsync(httpRequest, HttpResponse.BodyHandlers.discarding());
    }

    public static boolean invalidateRequest(HttpRequest httpRequest) {
        try {
            Optional<HttpCache> httpCache = Requester.HTTP_CLIENT.cache();

            if (httpCache.isEmpty()) {
                return false;
            }

            return httpCache.get().remove(httpRequest);
        } catch (Throwable th) {
            CommonUtils.LOGGER.warn("Failed to remove request from cache!", th);
            return false;
        }
    }

    public static <R, T> Stream<R> prepareParallelRequests(Stream<T> requests, Function<? super T, R> mapper) {
        return requests.map(request -> CompletableFuture.supplyAsync(
                () -> mapper.apply(request), CommonUtils.EXECUTOR
        )).toList().parallelStream().map(CompletableFuture::join);
    }

    public static HttpRequest.BodyPublisher ofObject(Object object, MediaType mediaType) {
        return HTTP_CLIENT.adapterCodec().orElseThrow().publisherOf(object, Utils.hintsOf(mediaType));
    }
}
