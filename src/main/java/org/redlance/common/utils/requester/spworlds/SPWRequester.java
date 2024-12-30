package org.redlance.common.utils.requester.spworlds;

import com.github.mizosoft.methanol.MediaType;
import com.github.mizosoft.methanol.MoreBodyPublishers;
import com.github.mizosoft.methanol.MultipartBodyPublisher;
import com.github.mizosoft.methanol.MutableRequest;
import com.github.mizosoft.methanol.TypeRef;
import org.redlance.common.utils.requester.Chunker;
import org.redlance.common.utils.requester.Requester;
import org.redlance.common.utils.requester.spworlds.obj.GroupObject;
import org.redlance.common.utils.requester.spworlds.obj.PostObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

public class SPWRequester {
    public static void authDiscord(String code) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://spworlds.ru/api/auth/discord"))
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"code\":\"" + code + "\"}"))
                .build();

        Requester.sendRequestVoid(request);
    }

    public static void createPost(String groupId, String text, HttpRequest.BodyPublisher bodyPublisher, MediaType mediaType) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://spworlds.ru/api/sp/posts/image?groupId=" + groupId))
                .header("content-type", "multipart/form-data")
                .POST(MultipartBodyPublisher.newBuilder()
                        .textPart("text", text)
                        .formPart("image", "",
                                MoreBodyPublishers.ofMediaType(bodyPublisher, mediaType)
                        ).build()
                ).build();

        Requester.sendRequestVoidAsync(request);
    }

    public static List<PostObject> requestPosts(String sort, String source, long time, int maxPages) throws IOException, InterruptedException {
        List<PostObject> postObjects = new ArrayList<>();
        requestPostsChuncking(postObjects::addAll, sort, source, time, maxPages);
        return postObjects;
    }

    public static <T> T requestPostsChuncking(Chunker<T, List<PostObject>> chunker, String sort, String source, long time, int maxPages) throws IOException, InterruptedException {
        for (int page = 1; page <= maxPages; page++) {
            HttpRequest request = MutableRequest.GET(String.format(
                    "https://spworlds.ru/api/sp/posts?sort=%s&source=%s&p=%s&time=%s", sort, source, page, time
            ));

            List<PostObject> postObjects = Requester.sendRequest(request, new TypeRef<>() {});

            T ret = chunker.apply(postObjects);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    public static List<GroupObject> requestGroups(int maxPages) throws IOException, InterruptedException {
        List<GroupObject> groupObjects = new ArrayList<>();
        requestGroupsChuncking(groupObjects::addAll, maxPages);
        return groupObjects;
    }

    public static <T> T requestGroupsChuncking(Chunker<T, List<GroupObject>> chunker, int maxPages) throws IOException, InterruptedException {
        for (int page = 1; page <= maxPages; page++) {
            HttpRequest request = MutableRequest.GET(String.format(
                    "https://spworlds.ru/api/sp/groups?type=following&p=%s", page
            ));

            List<GroupObject> postObjects = Requester.sendRequest(request, new TypeRef<>() {});

            T ret = chunker.apply(postObjects);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    public static void upvotePost(String postId, boolean up) throws IOException, InterruptedException {
        votePost(postId, true, up);
    }

    public static void votePost(String postId, boolean vote, boolean up) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://spworlds.ru/api/sp/posts/" + postId))
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"vote\": " + vote + ", \"isUpvote\": " + up + "}"))
                .build();

        Requester.sendRequestVoid(request);
    }
}
