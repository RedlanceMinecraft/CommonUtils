package org.redlance.common.utils.requester.boosty;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.github.kosmx.emotes.server.config.Serializer;
import org.redlance.common.utils.requester.Requester;
import org.redlance.common.utils.requester.boosty.obj.BoostyUser;
import org.redlance.common.utils.requester.boosty.obj.PostSale;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class BoostyRequester {

    @SuppressWarnings("unchecked")
    public static List<PostSale> requestPostSales(String blog, String token) throws ExecutionException {
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
    public static List<BoostyUser> requestSubscribers(String blog, String token, int levelId) throws ExecutionException {
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

    public static BoostyUser requestUserProfile(String blog, String token, int userId) throws ExecutionException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.boosty.to/v1/blog/" + blog + "/subscriber/" + userId + "/profile"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        JsonObject response = Requester.sendRequest(httpRequest, JsonObject.class);
        if (!response.has("data")) {
            throw new NullPointerException(response.toString());
        }

        JsonObject data = response.getAsJsonObject("data");
        if (!data.has("profile")) {
            throw new NullPointerException(data.toString());
        }

        JsonObject profile = data.getAsJsonObject("profile");
        if (!profile.has("user")) {
            throw new NullPointerException(profile.toString());
        }

        return Serializer.serializer.fromJson(
                profile.getAsJsonObject("user"), BoostyUser.class
        );
    }

    public static int requestUserFromDialog(String token, int dialogId) throws ExecutionException {
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
}
