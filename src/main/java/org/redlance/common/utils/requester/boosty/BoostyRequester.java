package org.redlance.common.utils.requester.boosty;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.github.kosmx.emotes.server.config.Serializer;
import org.redlance.common.utils.requester.Requester;
import org.redlance.common.utils.requester.boosty.obj.profile.BoostyProfile;
import org.redlance.common.utils.requester.boosty.obj.user.BoostyUser;
import org.redlance.common.utils.requester.boosty.obj.post.PostSale;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;

public class BoostyRequester {

    @SuppressWarnings("unchecked")
    public static List<PostSale> requestPostSales(String blog, String token) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.boosty.to/v1/blog/" + blog + "/sales/post/?limit=300"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        JsonObject response = Requester.sendRequest(httpRequest, JsonObject.class);
        if (!response.has("data")) {
            throw new NullPointerException(response.toString());
        }

        JsonObject data = response.getAsJsonObject("data");
        if (!data.has("postsSales")) {
            throw new NullPointerException(data.toString());
        }

        return Serializer.serializer.fromJson(data.getAsJsonArray("postsSales"),
                (TypeToken<List<PostSale>>) TypeToken.getParameterized(List.class, PostSale.class)
        );
    }

    @SuppressWarnings("unchecked")
    public static List<BoostyUser> requestSubscribers(String blog, String token, int levelId) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.boosty.to/v1/blog/" + blog + "/subscribers?limit=300&is_active=true&level_ids=" + levelId))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        JsonObject response = Requester.sendRequest(httpRequest, JsonObject.class);
        if (!response.has("data")) {
            throw new NullPointerException(response.toString());
        }

        return Serializer.serializer.fromJson(response.getAsJsonArray("data"),
                (TypeToken<List<BoostyUser>>) TypeToken.getParameterized(List.class, BoostyUser.class)
        );
    }

    public static BoostyProfile requestUserProfile(String blog, String token, int userId) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.boosty.to/v1/blog/" + blog + "/subscriber/" + userId + "/profile"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        JsonObject response = Requester.sendRequest(httpRequest, JsonObject.class);
        if (!response.has("data")) {
            throw new NullPointerException(response.toString());
        }

        JsonObject data = response.getAsJsonObject("data");
        if (!data.has("profile")) {
            throw new NullPointerException(data.toString());
        }

        return Serializer.serializer.fromJson(
                data.getAsJsonObject("profile"), BoostyProfile.class
        );
    }

    public static int requestUserFromDialog(String token, int dialogId) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.boosty.to/v1/dialog/" + dialogId))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        JsonObject response = Requester.sendRequest(httpRequest, JsonObject.class);
        if (!response.has("chatmate")) {
            throw new NullPointerException(response.toString());
        }

        JsonObject chatmate = response.getAsJsonObject("chatmate");
        if (!chatmate.has("id")) {
            throw new NullPointerException(chatmate.toString());
        }

        return chatmate.get("id").getAsInt();
    }

    public static String requestSocketToken(String token) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.boosty.to/v1/ws/connect"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        JsonObject response = Requester.sendRequest(httpRequest, JsonObject.class);
        if (!response.has("token")) {
            throw new NullPointerException(response.toString());
        }

        return response.get("token").getAsString();
    }
}
