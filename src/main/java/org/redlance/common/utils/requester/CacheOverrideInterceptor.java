package org.redlance.common.utils.requester;

import com.github.mizosoft.methanol.CacheControl;
import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.ResponseBuilder;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * <a href="https://github.com/mizosoft/methanol/issues/94">methanol#94</a>
 */
public final class CacheOverrideInterceptor implements Methanol.Interceptor {
    private static final String CACHE_CONTROL_HEADER = "Cache-Control";

    @Override
    public <T> HttpResponse<T> intercept(HttpRequest request, Chain<T> chain) throws IOException, InterruptedException {
        return forceCache(chain.forward(request));
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> interceptAsync(HttpRequest request, Chain<T> chain) {
        return chain.forwardAsync(request).thenApply(this::forceCache);
    }

    private <T> HttpResponse<T> forceCache(HttpResponse<T> response) {
        long maxAge = CacheControl.parse(response.headers()).maxAge()
                .map(Duration::toSeconds)
                .orElse(0L);

        return ResponseBuilder.from(response)
                .setHeader(CACHE_CONTROL_HEADER, "max-age=" + Math.max(maxAge, 15))
                .build();
    }
}
