package org.redlance.common.utils.requester.interceptors;

import com.github.mizosoft.methanol.Methanol;
import org.redlance.common.utils.requester.Requester;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CookieFixerInterceptor implements Methanol.Interceptor {
    @Override
    public <T> HttpResponse<T> intercept(HttpRequest request, Chain<T> chain) throws IOException, InterruptedException {
        downgradleCookies(request);
        return chain.forward(request);
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> interceptAsync(HttpRequest request, Chain<T> chain) {
        downgradleCookies(request);
        return chain.forwardAsync(request);
    }

    private void downgradleCookies(HttpRequest request) {
        CookieHandler handler = Requester.HTTP_CLIENT.cookieHandler().orElse(null);

        if (handler instanceof CookieManager manager) {
            List<HttpCookie> cookies = manager.getCookieStore().get(request.uri());

            for (HttpCookie cookie : cookies) {
                cookie.setVersion(0); // idk why
            }
        }
    }
}
