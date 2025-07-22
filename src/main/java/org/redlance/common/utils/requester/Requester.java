package org.redlance.common.utils.requester;

import com.github.mizosoft.methanol.*;
import com.github.mizosoft.methanol.adapter.gson.GsonAdapterFactory;
import com.github.mizosoft.methanol.internal.Utils;
import io.github.kosmx.emotes.server.config.Serializer;
import org.redlance.common.CommonUtils;
import org.redlance.common.utils.LambdaExceptionUtils;
import org.redlance.common.utils.requester.interceptors.CacheOverrideInterceptor;
import org.redlance.common.utils.requester.interceptors.CookieFixerInterceptor;
import org.redlance.common.utils.requester.interceptors.FallbackInterceptor;

import java.io.IOException;
import java.net.CookieManager;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class Requester {
    public static final Methanol HTTP_CLIENT = Methanol.newBuilder()
            .executor(CommonUtils.createExecutor("http-requester-"))
            .connectTimeout(Duration.ofMinutes(1))
            .version(HttpClient.Version.HTTP_2)
            .proxy(UrlProxySelector.INSTANCE)
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .cache(HttpCache.newBuilder()
                    .executor(CommonUtils.createExecutor("http-cache-"))
                    .cacheOnMemory(1024 * 1024 * 1024) // 1024 MBs
                    .listener(new HttpCache.Listener() {
                        @Override
                        public void onNetworkUse(HttpRequest request, TrackedResponse<?> cacheResponse) {
                            CommonUtils.LOGGER.debug("Network used: {}", request);
                        }
                    })
                    .build()
            )
            .interceptor(new CookieFixerInterceptor())
            .interceptor(new FallbackInterceptor())
            .backendInterceptor(new CacheOverrideInterceptor())
            .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
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

    private static final ExecutorService PARALLEL_REQUESTER = CommonUtils.createExecutor("parallel-requester-");
    public static <R, T> Stream<R> prepareParallelRequests(Stream<T> requests, Function<? super T, R> mapper) {
        return requests.map(request -> Requester.PARALLEL_REQUESTER.submit(
                () -> mapper.apply(request)
        )).map(LambdaExceptionUtils.rethrowFunction((Future::get)));
    }

    public static HttpRequest.BodyPublisher ofObject(Object object, MediaType mediaType) {
        return HTTP_CLIENT.adapterCodec().orElseThrow().publisherOf(object, Utils.hintsOf(mediaType));
    }
}
