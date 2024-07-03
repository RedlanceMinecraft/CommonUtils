package org.redlance.common.utils.requester.discord;

import com.google.gson.JsonObject;
import org.redlance.common.utils.requester.Requester;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

public class DiscordRequester {
    public static String authorizeApp(String userToken, long clientId, String redirectUri) throws ExecutionException {
        return authorizeApp(userToken, clientId, "code", redirectUri, "identify");
    }

    public static String authorizeApp(String userToken, long clientId, String responseType, String redirectUri, String scope) throws ExecutionException {
        String uri = String.format("https://discord.com/api/v9/oauth2/authorize?client_id=%s&response_type=%s&redirect_uri=%s&scope=%s",
                clientId, responseType, URLEncoder.encode(redirectUri, StandardCharsets.UTF_8), scope
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("content-type", "application/json")
                .header("authorization", userToken)
                .POST(HttpRequest.BodyPublishers.ofString("{\"authorize\":true}"))
                .build();

        Requester.refreshRequest(request);

        JsonObject obj = Requester.sendRequest(request, JsonObject.class);
        if (!obj.has("location")) {
            throw new NullPointerException(obj.toString());
        }

        return URI.create(obj.get("location").getAsString()).getQuery()
                .split("=")[1];
    }
}
