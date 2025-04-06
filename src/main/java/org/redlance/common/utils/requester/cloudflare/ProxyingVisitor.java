package org.redlance.common.utils.requester.cloudflare;

import com.github.mizosoft.methanol.MediaType;
import com.github.mizosoft.methanol.Methanol;
import org.redlance.common.utils.requester.JavaLinkVisitor;
import org.redlance.common.utils.requester.Requester;
import webGrude.http.GetException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ProxyingVisitor extends JavaLinkVisitor {
    protected final URI proxyUrl;

    public ProxyingVisitor(Methanol client, URI proxyUrl) {
        super(client);
        this.proxyUrl = proxyUrl;
    }

    @Override
    public String getPage(String url) {
        HttpRequest request = HttpRequest.newBuilder(proxyUrl)
                .POST(Requester.ofObject(new ProxyRequest(url), MediaType.APPLICATION_JSON))
                .header("Content-Type", "application/json")
                .build();

        try {
            HttpResponse<ProxyResponse> response = ((Methanol) this.httpClient).send(request, ProxyResponse.class);
            if (response.body().code() != 200) {
                throw new IOException("Unsuccessful response " + response.body().code());
            }
            return response.body().source();
        } catch (IOException | InterruptedException e) {
            throw new GetException(e, url);
        }
    }

    public record ProxyRequest(String url, String mode) {
        public ProxyRequest(String url) {
            this(url, "source");
        }
    }

    public record ProxyResponse(String source, int code) { }
}
