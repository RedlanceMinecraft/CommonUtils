package org.redlance.common.utils.requester;

import webGrude.OkHttpBrowser;
import webGrude.http.GetException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class JavaLinkVisitor extends OkHttpBrowser {
    protected final HttpClient httpClient;

    public JavaLinkVisitor(HttpClient client) {
        this.httpClient = client;
    }

    @Override
    public String getPage(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).build();
        try {
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new GetException(e, url);
        }
    }
}
