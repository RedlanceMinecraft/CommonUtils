package org.redlance.common.requester;

import com.github.mizosoft.methanol.AdapterCodec;
import com.github.mizosoft.methanol.HttpCache;
import com.github.mizosoft.methanol.MediaType;
import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.TrackedResponse;
import com.github.mizosoft.methanol.adapter.jackson3.JacksonAdapterFactory;
import org.redlance.common.jackson.JacksonMappers;
import org.redlance.common.utils.CommonExecutors;
import org.redlance.common.utils.LambdaExceptionUtils;

import java.net.CookieManager;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.file.Path;
import java.time.Duration;

/**
 * What every HTTP client shares: proxies, redirects, the user agent, cookies, and the JSON and Smile codecs; the cache
 * on disk for the clients that add it.
 * <p>
 * Each requester builds a client of its own from {@link #newBuilder(String)}, with its own executor, and adds only what
 * it needs, so that one requester's needs, like the {@link #CACHE} or
 * {@link org.redlance.common.requester.interceptors.CacheOverrideInterceptor}, do not reach the others.
 */
public final class HttpClients {
    public static final MediaType APPLICATION_JACKSON_SMILE = MediaType.of("application", "x-jackson-smile");

    /**
     * Responses cached on disk; one store for every client that adds it with {@code builder.cache(CACHE)}, as the
     * directory takes one at a time. A client that receives personal data leaves it out.
     */
    public static final HttpCache CACHE = HttpCache.newBuilder()
            .executor(CommonExecutors.createExecutor("http-cache-"))
            .cacheOnDisk(Path.of("httpcache"), 1024 * 1024 * 1024) // 1024 MBs
            .listener(new HttpCache.Listener() {
                @Override
                public void onNetworkUse(HttpRequest request, TrackedResponse<?> cacheResponse) {
                    RequesterUtils.LOGGER.debug("Network used: {}", request);
                }
            })
            .build();

    private static final CookieManager COOKIES = new CookieManager();

    private static final AdapterCodec CODEC = AdapterCodec.newBuilder()
            // Json
            .decoder(JacksonAdapterFactory.createJsonDecoder(JacksonMappers.OBJECT_MAPPER))
            .encoder(JacksonAdapterFactory.createJsonEncoder(JacksonMappers.OBJECT_MAPPER))

            // Smile
            .decoder(JacksonAdapterFactory.createDecoder(JacksonMappers.SMILE_MAPPER, APPLICATION_JACKSON_SMILE))
            .encoder(JacksonAdapterFactory.createEncoder(JacksonMappers.SMILE_MAPPER, APPLICATION_JACKSON_SMILE))
            .build();

    static {
        RequesterUtils.LOGGER.debug("Added http cache saving hook!");
        Runtime.getRuntime().addShutdownHook(new Thread(
                LambdaExceptionUtils.rethrowRunnable(HttpClients.CACHE::close), "http-cache-saver"
        ));
    }

    private HttpClients() {
    }

    /**
     * @param name Name of the client, which its executor names its threads after.
     * @return A builder with the shared settings and an executor of its own, for the client of one requester; it caches
     * nothing until given the {@link #CACHE}.
     */
    public static Methanol.Builder newBuilder(String name) {
        return Methanol.newBuilder()
                .executor(CommonExecutors.createExecutor(name + "-"))
                .connectTimeout(Duration.ofMinutes(1))
                .version(HttpClient.Version.HTTP_1_1)
                .proxy(UrlProxySelector.INSTANCE)
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/155.0.0.0 Safari/537.36")
                .cookieHandler(COOKIES)
                .adapterCodec(CODEC);
    }
}
