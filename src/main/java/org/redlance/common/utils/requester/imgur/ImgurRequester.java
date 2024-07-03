package org.redlance.common.utils.requester.imgur;

import com.google.gson.JsonObject;
import org.redlance.common.utils.requester.Requester;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ExecutionException;

public class ImgurRequester {
    public static String saveToImgur(String token, byte[] byteArray) throws ExecutionException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.imgur.com/3/image"))
                .header("Authorization", "Client-ID " + token)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("image=" + URLEncoder.encode(
                        Base64.getEncoder().encodeToString(byteArray), StandardCharsets.UTF_8
                )))
                .build();

        Requester.refreshRequest(request);

        JsonObject response = Requester.sendRequest(request, JsonObject.class);
        if (!response.has("data")) {
            throw new NullPointerException(response.toString());
        }

        JsonObject data = response.getAsJsonObject("data");
        if (!data.has("link")) {
            throw new NullPointerException(data.toString());
        }

        return data.get("link").getAsString();
    }
}
