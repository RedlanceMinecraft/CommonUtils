package org.redlance.common.utils.requester.reposilite;

import com.github.mizosoft.methanol.HttpStatus;
import org.redlance.common.utils.requester.Requester;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@SuppressWarnings("unused")
public class ReposiliteRequester {
    public static int uploadFile(String url, String auth, HttpRequest.BodyPublisher bodyPublisher) throws IOException, InterruptedException {
        return uploadFile(URI.create(url), auth, bodyPublisher);
    }

    public static int uploadFile(URI uri, String auth, HttpRequest.BodyPublisher bodyPublisher) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8)))
                .header("Content-Type", "application/octet-stream")
                .PUT(bodyPublisher)
                .build();

        Requester.invalidateRequest(request);
        return Requester.sendRequestVoid(request).statusCode();
    }
}
