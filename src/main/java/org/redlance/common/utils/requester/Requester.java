package org.redlance.common.utils.requester;

import com.github.mizosoft.methanol.Methanol;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.reflect.TypeToken;
import io.github.kosmx.emotes.server.config.Serializer;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.redlance.common.CommonUtils;

import java.net.CookieManager;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Requester {
    public static final HttpClient HTTP_CLIENT = Methanol.newBuilder()
            .executor(CommonUtils.EXECUTOR)
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .userAgent("Java/21 (On dima_dencep's pc)")
            .cookieHandler(new CookieManager())
            .build();

    private static final CacheLoader<HttpRequest, String> CACHE_LOADER = new CacheLoader<>() {
        @Override
        public @NotNull String load(@NotNull HttpRequest httpRequest) throws Exception {
            String response = Requester.HTTP_CLIENT // send request
                    .send(httpRequest, HttpResponse.BodyHandlers.ofString())
                    .body();

            if (StringUtils.isBlank(response)) {
                invalidateRequest(httpRequest);
                throw new NullPointerException("Invalid response!");
            }

            return response;
        }
    };

    private static final LoadingCache<HttpRequest, String> CACHE = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(CACHE_LOADER);

    public static <T> @NotNull T sendRequest(HttpRequest httpRequest, Class<T> token) throws ExecutionException {
        return sendRequest(httpRequest, TypeToken.get(token));
    }

    public static <T> @NotNull T sendRequest(HttpRequest httpRequest, TypeToken<T> token) throws ExecutionException {
        String response = sendRequest(httpRequest);

        T serialized = Serializer.serializer.fromJson(response, token); // serialize
        if (serialized == null) {
            invalidateRequest(httpRequest);
            throw new NullPointerException("Invalid serialized result!");
        }

        return serialized;
    }

    public static String sendRequest(HttpRequest httpRequest) throws ExecutionException {
        return CACHE.get(httpRequest);
    }

    public static void invalidateRequest(HttpRequest httpRequest) {
        CACHE.invalidate(httpRequest);
    }
}
