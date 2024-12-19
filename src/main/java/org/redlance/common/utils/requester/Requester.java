package org.redlance.common.utils.requester;

import com.github.mizosoft.methanol.HttpCache;
import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.MoreBodyHandlers;
import com.github.mizosoft.methanol.TrackedResponse;
import com.github.mizosoft.methanol.TypeRef;
import org.jetbrains.annotations.NotNull;
import org.redlance.common.CommonUtils;

import java.io.IOException;
import java.net.CookieManager;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Requester {
    public static final Methanol HTTP_CLIENT = Methanol.newBuilder()
            .executor(CommonUtils.EXECUTOR)
            .connectTimeout(Duration.ofMinutes(1))
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .cache(HttpCache.newBuilder()
                    .executor(CommonUtils.EXECUTOR)
                    .cacheOnMemory(1000 * 1024 * 1024) // 1000 MBs
                    .listener(new HttpCache.Listener() {
                        @Override
                        public void onNetworkUse(HttpRequest request, TrackedResponse<?> cacheResponse) {
                            CommonUtils.LOGGER.debug("Nework used: {}", request);
                        }
                    })
                    .build()
            )
            .backendInterceptor(new CacheOverrideInterceptor())
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36")
            .cookieHandler(new CookieManager())
            .build();

    public static <T> @NotNull T sendRequest(HttpRequest httpRequest, Class<T> token) throws IOException, InterruptedException {
        return sendRequest(httpRequest, TypeRef.from(token));
    }

    public static <T> @NotNull CompletableFuture<T> sendRequestAsync(HttpRequest httpRequest, Class<T> token) {
        return sendRequestAsync(httpRequest, TypeRef.from(token));
    }

    public static <T> @NotNull T sendRequest(HttpRequest httpRequest, TypeRef<T> token) throws IOException, InterruptedException {
        T serialized = sendRequest(httpRequest, MoreBodyHandlers.ofObject(token)); // serialize

        if (serialized == null) {
            invalidateRequest(httpRequest);
            throw new NullPointerException("Invalid serialized result!");
        }

        return serialized;
    }

    public static <T> @NotNull CompletableFuture<T> sendRequestAsync(HttpRequest httpRequest, TypeRef<T> token) {
        return sendRequestAsync(httpRequest, MoreBodyHandlers.ofObject(token))
                .whenComplete((serialized, throwable) -> {
                    if (serialized == null && throwable == null) {
                        invalidateRequest(httpRequest);
                        throw new NullPointerException("Invalid serialized result!");
                    }
                });
    }

    public static <T> T sendRequest(HttpRequest httpRequest, HttpResponse.BodyHandler<T> bodyHandler) throws IOException, InterruptedException {
        return Requester.HTTP_CLIENT // send request
                .send(httpRequest, bodyHandler)
                .body();
    }

    public static <T> CompletableFuture<T> sendRequestAsync(HttpRequest httpRequest, HttpResponse.BodyHandler<T> bodyHandler) {
        return Requester.HTTP_CLIENT // send request
                .sendAsync(httpRequest, bodyHandler)
                .thenApply(HttpResponse::body);
    }

    public static void sendRequest(HttpRequest httpRequest) throws IOException, InterruptedException {
        sendRequest(httpRequest, HttpResponse.BodyHandlers.discarding());
    }

    public static CompletableFuture<Void> sendRequestAsync(HttpRequest httpRequest) {
        return sendRequestAsync(httpRequest, HttpResponse.BodyHandlers.discarding());
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

    public static <R, T> Stream<R> prepareParallelRequests(Stream<T> requests, Function<? super T, Supplier<R>> mapper) {
        return prepareParallelRequests(requests.map(mapper).toList());
    }

    public static <R> Stream<R> prepareParallelRequests(List<Supplier<R>> requests) {
        final List<CompletableFuture<R>> futures = new ArrayList<>();

        for (Supplier<R> request : requests) {
            futures.add(CompletableFuture.supplyAsync(request, CommonUtils.EXECUTOR));
        }

        return futures.parallelStream().map(CompletableFuture::join);
    }
}
