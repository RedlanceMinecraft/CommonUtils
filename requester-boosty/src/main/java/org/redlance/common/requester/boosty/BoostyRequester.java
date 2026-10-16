package org.redlance.common.requester.boosty;

import com.github.mizosoft.methanol.Methanol;
import org.jetbrains.annotations.Nullable;
import org.redlance.common.requester.Chunker;
import org.redlance.common.requester.HttpClients;
import org.redlance.common.requester.boosty.obj.donation.Donation;
import org.redlance.common.requester.boosty.obj.post.PostSale;
import org.redlance.common.requester.boosty.obj.post.PostSales;
import org.redlance.common.requester.boosty.obj.user.BoostyProfileDeserializer;
import org.redlance.common.requester.boosty.obj.user.BoostyUser;
import org.redlance.common.requester.boosty.paginators.BoostyLegacyPaginator;
import org.redlance.common.requester.boosty.paginators.BoostyPaginator;
import tools.jackson.databind.annotation.JsonDeserialize;
import com.github.mizosoft.methanol.TypeRef;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * Requests to the Boosty API on behalf of a blog owner.
 * <p>
 * {@code blog} is the blog name, as in {@code boosty.to/<blog>}; {@code token} is the owner's access token, see
 * {@link BoostySessionStorage}. Paged requests hand every page to a {@link Chunker}: a non-null result stops the walk
 * and is returned, {@code null} after the last page. Boosty errors are thrown as {@link BoostyRequestException}.
 */
public class BoostyRequester {
    /**
     * Client of the Boosty API: the shared settings without the cache on disk, as every answer of Boosty is the current
     * state and holds personal data.
     */
    static final Methanol HTTP_CLIENT = HttpClients.newBuilder("http-boosty").build();

    /**
     * How long a request waits for Boosty to answer.
     */
    static final Duration TIMEOUT = Duration.ofSeconds(30);

    /**
     * @deprecated Use {@link BoostyRequester#requestPostSalesChunking(Chunker, String, String)}
     */
    @Deprecated(forRemoval = true)
    public static List<PostSale> requestPostSales(String blog, String token) throws IOException, InterruptedException {
        List<PostSale> postSalesList = new ArrayList<>();
        requestPostSalesChunking(postSales -> {
            postSalesList.addAll(postSales.postsSales());
            return null;
        }, blog, token);
        return postSalesList;
    }

    /**
     * Walks the sales of the blog's paid posts, up to 300 per page, the latest first.
     */
    public static <T> T requestPostSalesChunking(Chunker<T, PostSales> chunker, String blog, String token) throws IOException, InterruptedException {
        return Chunker.sendChunkingRequest(HTTP_CLIENT, chunker, paginator -> {
            String url = String.format("https://api.boosty.to/v1/blog/%s/sales/post/?limit=300&offset=%s&sort_by=time&order=gt",
                    blog, paginator == null ? 0 : paginator.offset()
            );

            return HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .timeout(TIMEOUT)
                    .build();
        }, new TypeRef<BoostyPaginator<PostSales>>() {});
    }

    /**
     * Walks the donations to the blog, up to 300 per page, the latest first.
     */
    public static <T> T requestDonationsChunking(Chunker<T, List<Donation>> chunker, String blog, String token) throws IOException, InterruptedException {
        return requestDonationsChunking(chunker, blog, token, null, 0);
    }

    /**
     * Walks the donations to the blog that went to one kind of target or to one goal or post, up to 300 per page, the
     * latest first.
     *
     * @param type     Kind of the donations; every kind when {@code null}.
     * @param targetId Only the donations to this goal or post, see {@link Donation#targetId()}; all of them when
     *                 {@code 0}.
     * @throws IllegalArgumentException If {@code type} is {@link Donation.Type#UNKNOWN}, which Boosty cannot filter by.
     */
    public static <T> T requestDonationsChunking(Chunker<T, List<Donation>> chunker, String blog, String token,
                                                 Donation.@Nullable Type type, long targetId) throws IOException, InterruptedException {
        if (type == Donation.Type.UNKNOWN) {
            throw new IllegalArgumentException("Boosty filters donations only by the kinds it knows");
        }

        // Boosty rejects a kind it does not know with invalid_param
        String filter = (type == null ? "" : "&type=" + type.name().toLowerCase(Locale.ROOT))
                + (targetId == 0 ? "" : "&target_id=" + targetId);

        return Chunker.sendChunkingRequest(HTTP_CLIENT, (Donations page) -> chunker.apply(page.donationsSales()), paginator -> {
            String url = String.format("https://api.boosty.to/v1/blog/%s/sales/donation/?limit=300&offset=%s&sort_by=time&order=gt%s",
                    blog, paginator == null ? 0 : paginator.offset(), filter
            );

            return HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .timeout(TIMEOUT)
                    .build();
        }, new TypeRef<BoostyPaginator<Donations>>() {});
    }

    /**
     * @deprecated Use {@link BoostyRequester#requestSubscribersChunking(Chunker, String, String, long[])}
     */
    @Deprecated(forRemoval = true)
    public static List<BoostyUser> requestSubscribers(String blog, String token, long levelId) throws IOException, InterruptedException {
        List<BoostyUser> boostyUsers = new ArrayList<>();
        requestSubscribersChunking(page -> {
            boostyUsers.addAll(page);
            return null;
        }, blog, token, levelId);
        return boostyUsers;
    }

    /**
     * Walks the blog's active subscribers, up to 300 per page, the latest subscription first.
     * <p>
     * Pages are cut by offset, so a subscription that starts or ends during the walk shifts them: a subscriber may
     * come twice, or be missed until the next walk.
     *
     * @param levelIds Levels to include; every level when empty.
     */
    public static <T> T requestSubscribersChunking(Chunker<T, List<BoostyUser>> chunker, String blog, String token, long... levelIds) throws IOException, InterruptedException {
        String levels = levelIds.length == 0 ? "" : LongStream.of(levelIds)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(",", "&level_ids=", ""));

        return Chunker.sendChunkingRequest(HTTP_CLIENT, chunker, paginator -> {
            String url = String.format("https://api.boosty.to/v1/blog/%s/subscribers?sort_by=on_time&limit=300&status=active%s&offset=%s&order=gt",
                    blog, levels, paginator == null ? 0 : paginator.offset()
            );

            return HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .timeout(TIMEOUT)
                    .build();
        }, new TypeRef<BoostyLegacyPaginator<List<BoostyUser>>>() {});
    }

    /**
     * Requests the profile of a user as the blog owner sees it, read into a {@link BoostyUser} by
     * {@link BoostyProfileDeserializer}.
     *
     * @param userId Any Boosty user; one who never subscribed to the blog comes without a subscription.
     * @throws BoostyRequestException If Boosty rejected the request, e.g. with {@code user_not_found}.
     */
    public static BoostyUser requestUserProfile(String blog, String token, long userId) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.boosty.to/v1/blog/" + blog + "/subscriber/" + userId + "/profile"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .timeout(TIMEOUT)
                .build();

        ProfileResponse response = HTTP_CLIENT.send(httpRequest, ProfileResponse.class).body();
        BoostyUser profile = response == null || response.data() == null ? null : response.data().profile();
        if (profile == null || profile.id() == 0) {
            throw failure(response, "Boosty sent no profile of user " + userId);
        }

        return profile;
    }

    /**
     * Requests the id of the user on the other side of a dialog.
     *
     * @throws BoostyRequestException If Boosty rejected the request.
     */
    public static long requestUserFromDialog(String token, long dialogId) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.boosty.to/v1/dialog/" + dialogId))
                .header("Authorization", "Bearer " + token)
                .GET()
                .timeout(TIMEOUT)
                .build();

        DialogResponse response = HTTP_CLIENT.send(httpRequest, DialogResponse.class).body();
        if (response == null || response.chatmate() == null || response.chatmate().id() == 0) {
            throw failure(response, "Boosty sent no chatmate of dialog " + dialogId);
        }

        return response.chatmate().id();
    }

    /**
     * Requests a token for connecting to the pubsub websocket, see {@link BoostyWebsocketListener}.
     *
     * @throws BoostyRequestException If Boosty rejected the request.
     */
    public static String requestSocketToken(String token) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.boosty.to/v1/ws/connect"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .timeout(TIMEOUT)
                .build();

        SocketTokenResponse response = HTTP_CLIENT.send(httpRequest, SocketTokenResponse.class).body();
        if (response == null || response.token() == null) {
            throw failure(response, "Boosty sent no socket token");
        }

        return response.token();
    }

    /**
     * @param response The response; {@code null} when Boosty sent none.
     * @param missing  What the response lacks, for the message.
     */
    private static BoostyRequestException failure(@Nullable BoostyErrorResponse response, String missing) {
        return response == null ? new BoostyRequestException(missing, null, null) : response.failure(missing);
    }

    /**
     * Content of a donations page: {@code {"donationsSales": [...]}}.
     *
     * @param donationsSales Donations on the page.
     */
    record Donations(List<Donation> donationsSales) {
        Donations {
            donationsSales = donationsSales == null ? List.of() : Collections.unmodifiableList(donationsSales);
        }
    }

    /**
     * {@code GET /v1/blog/<blog>/subscriber/<id>/profile}: {@code {"data": {"profile": {...}}}}.
     *
     * @param data             The content; {@code null} with an error.
     * @param error            Error code, e.g. {@code user_not_found}.
     * @param errorDescription Readable error text.
     */
    record ProfileResponse(
            @Nullable ProfileData data,
            @Nullable String error,
            @Nullable String errorDescription
    ) implements BoostyErrorResponse {
    }

    /**
     * Content of the profile response.
     *
     * @param profile The profile; {@code null} when it has no user.
     */
    record ProfileData(@JsonDeserialize(using = BoostyProfileDeserializer.class) @Nullable BoostyUser profile) {
    }

    /**
     * {@code GET /v1/dialog/<id>}: the dialog with its messages; only the chatmate's id is read.
     *
     * @param chatmate         The user on the other side; {@code null} with an error.
     * @param error            Error code, e.g. {@code unauthorized}.
     * @param errorDescription Readable error text.
     */
    record DialogResponse(
            @Nullable Chatmate chatmate,
            @Nullable String error,
            @Nullable String errorDescription
    ) implements BoostyErrorResponse {
    }

    /**
     * The user on the other side of a dialog.
     *
     * @param id User id.
     */
    record Chatmate(long id) {
    }

    /**
     * {@code GET /v1/ws/connect}: {@code {"token": ...}}.
     *
     * @param token            Token for {@link BoostyWebsocketListener}; {@code null} with an error.
     * @param error            Error code, e.g. {@code unauthorized}.
     * @param errorDescription Readable error text.
     */
    record SocketTokenResponse(
            @Nullable String token,
            @Nullable String error,
            @Nullable String errorDescription
    ) implements BoostyErrorResponse {
    }
}
