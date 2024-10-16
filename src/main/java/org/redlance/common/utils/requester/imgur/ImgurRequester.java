package org.redlance.common.utils.requester.imgur;

import com.github.mizosoft.methanol.MediaType;
import com.github.mizosoft.methanol.MoreBodyPublishers;
import com.github.mizosoft.methanol.MultipartBodyPublisher;
import com.google.gson.JsonObject;
import org.redlance.common.utils.requester.Requester;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;

public class ImgurRequester {
    public static String saveToImgur(String token, HttpRequest.BodyPublisher bodyPublisher, MediaType mediaType) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.imgur.com/3/image"))
                .header("Authorization", "Client-ID " + token)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(MultipartBodyPublisher.newBuilder()
                        .formPart("image", "",
                                MoreBodyPublishers.ofMediaType(bodyPublisher, mediaType)
                        ).build()
                ).build();

        Requester.invalidateRequest(request);

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
