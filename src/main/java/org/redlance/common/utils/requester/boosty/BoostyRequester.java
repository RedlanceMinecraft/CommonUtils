package org.redlance.common.utils.requester.boosty;

import com.github.mizosoft.methanol.TypeRef;
import com.google.gson.JsonObject;
import io.github.kosmx.emotes.server.config.Serializer;
import org.redlance.common.utils.requester.Chunker;
import org.redlance.common.utils.requester.Requester;
import org.redlance.common.utils.requester.boosty.obj.post.PostSales;
import org.redlance.common.utils.requester.boosty.obj.profile.BoostyProfile;
import org.redlance.common.utils.requester.boosty.obj.user.BoostyUser;
import org.redlance.common.utils.requester.boosty.obj.post.PostSale;
import org.redlance.common.utils.requester.boosty.paginators.BoostyLegacyPaginator;
import org.redlance.common.utils.requester.boosty.paginators.BoostyPaginator;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class BoostyRequester {
    /**
     * @deprecated Use {@link BoostyRequester#requestPostSalesChunking(Chunker, String, String)}
     */
    @Deprecated(forRemoval = true)
    public static List<PostSale> requestPostSales(String blog, String token) throws IOException, InterruptedException {
        List<PostSale> postSalesList = new ArrayList<>();
        requestPostSalesChunking(postSales -> postSalesList.addAll(postSales.postsSales()), blog, token);
        return postSalesList;
    }

    public static <T> T requestPostSalesChunking(Chunker<T, PostSales> chunker, String blog, String token) throws IOException, InterruptedException {
        return Chunker.sendChunkingRequest(chunker, paginator -> {
            String url = String.format("https://api.boosty.to/v1/blog/%s/sales/post/?limit=300&offset=%s&sort_by=time&order=gt",
                    blog, paginator == null ? 0 : paginator.offset()
            );

            return HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
        }, new TypeRef<BoostyPaginator<PostSales>>() {});
    }

    /**
     * @deprecated Use {@link BoostyRequester#requestSubscribersChunking(Chunker, String, String, int)}
     */
    @Deprecated(forRemoval = true)
    public static List<BoostyUser> requestSubscribers(String blog, String token, int levelId) throws IOException, InterruptedException {
        List<BoostyUser> boostyUsers = new ArrayList<>();
        requestSubscribersChunking(boostyUsers::addAll, blog, token, levelId);
        return boostyUsers;
    }

    public static <T> T requestSubscribersChunking(Chunker<T, List<BoostyUser>> chunker, String blog, String token, int levelId) throws IOException, InterruptedException {
        return Chunker.sendChunkingRequest(chunker, paginator -> {
            String url = String.format("https://api.boosty.to/v1/blog/%s/subscribers?sort_by=on_time&limit=300&is_active=true&level_ids=%s&offset=%s&order=gt",
                    blog, levelId, paginator == null ? 0 : paginator.offset()
            );

            return HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .build();
        }, new TypeRef<BoostyLegacyPaginator<List<BoostyUser>>>() {});
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

        return Serializer.getSerializer().fromJson(
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
