package org.redlance.common.utils.requester;

import com.github.mizosoft.methanol.CacheControl;
import com.github.mizosoft.methanol.HttpCache;
import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.MoreBodyHandlers;
import com.github.mizosoft.methanol.MutableRequest;
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
import java.util.Optional;

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
                        public void onRequest(HttpRequest request) {
                            CommonUtils.LOGGER.info("Request received: {}", request);
                        }

                        @Override
                        public void onNetworkUse(HttpRequest request, TrackedResponse<?> cacheResponse) {
                            CommonUtils.LOGGER.info("Nework used: {}", request);
                        }
                    })
                    .build()
            )
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36")
            .cookieHandler(new CookieManager())
            .build();

    public static final CacheControl CACHE_CONTROL = CacheControl.newBuilder()
            .maxAge(Duration.ofSeconds(15))
            .build();

    public static <T> @NotNull T sendRequest(HttpRequest httpRequest, Class<T> token) throws IOException, InterruptedException {
        return sendRequest(httpRequest, TypeRef.from(token));
    }

    public static <T> @NotNull T sendRequest(HttpRequest httpRequest, TypeRef<T> token) throws IOException, InterruptedException {
        T serialized = sendRequest(httpRequest, MoreBodyHandlers.ofObject(token)); // serialize

        if (serialized == null) {
            invalidateRequest(httpRequest);
            throw new NullPointerException("Invalid serialized result!");
        }

        return serialized;
    }

    public static <T> T sendRequest(HttpRequest httpRequest, HttpResponse.BodyHandler<T> bodyHandler) throws IOException, InterruptedException {
        if (httpRequest.headers().firstValue("Cache-Control").isEmpty()) {
            CommonUtils.LOGGER.warn("Request without cache, manual setting...");

            httpRequest = MutableRequest.copyOf(httpRequest)
                    .cacheControl(Requester.CACHE_CONTROL)
                    .build();
        }

        return Requester.HTTP_CLIENT // send request
                .send(httpRequest, bodyHandler)
                .body();
    }

    public static String sendRequest(HttpRequest httpRequest) throws IOException, InterruptedException {
        return sendRequest(httpRequest, HttpResponse.BodyHandlers.ofString());
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
}
