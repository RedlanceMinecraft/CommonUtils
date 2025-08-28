/*package org.redlance.common.utils.requester.interceptors;

import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.MutableRequest;
import org.redlance.common.utils.requester.DisallowedHeadersCleaner;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class FallbackInterceptor implements Methanol.Interceptor {
    protected static final Map<String, String> RESOLVED = Map.of("spworlds.ru", "193.233.15.35");
    public static final HttpClient INSECURE_REQUESTER = createInsecureHttpClient();

    @Override
    public <T> HttpResponse<T> intercept(HttpRequest request, Chain<T> chain) throws IOException, InterruptedException {
        try {
            return chain.forward(request);
        } catch (SSLHandshakeException th) {
            String domain = request.uri().getHost();
            String ip = RESOLVED.get(domain);
            if (ip == null) throw th;

            MutableRequest mutable = MutableRequest.copyOf(request);
            mutable.setHeader("Host", domain);
            try {
                mutable.uri(replaceDomain(request.uri(), ip));
            } catch (URISyntaxException e) {
                throw new IOException("Failed to replace domain!", e);
            }

            DisallowedHeadersCleaner.clearHeaders();
            return INSECURE_REQUESTER.send(mutable, chain.bodyHandler());
        }
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> interceptAsync(HttpRequest request, Chain<T> chain) {
        return chain.forwardAsync(request); // TODO
    }

    private static HttpClient createInsecureHttpClient() {
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509ExtendedTrustManager() {
                    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {}
                    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {}
                    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {}
                    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {}
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                }
        };

        final SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
        } catch (Throwable th) {
            throw new RuntimeException(th);
        }

        SSLParameters sslParameters = new SSLParameters() {
            @Override
            public void setEndpointIdentificationAlgorithm(String algorithm) {
                super.setEndpointIdentificationAlgorithm(null);
            }
        };
        sslParameters.setEndpointIdentificationAlgorithm(null);
        return HttpClient.newBuilder().sslContext(sslContext).sslParameters(sslParameters).build();
    }

    public static URI replaceDomain(URI originalUri, String newHost) throws URISyntaxException {
        return new URI(originalUri.getScheme(), originalUri.getUserInfo(), newHost, originalUri.getPort(), originalUri.getPath(), originalUri.getQuery(), originalUri.getFragment());
    }
}*/
