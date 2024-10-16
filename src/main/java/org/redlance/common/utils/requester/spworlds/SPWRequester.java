package org.redlance.common.utils.requester.spworlds;

import com.github.mizosoft.methanol.MediaType;
import com.github.mizosoft.methanol.MoreBodyPublishers;
import com.github.mizosoft.methanol.MultipartBodyPublisher;
import com.github.mizosoft.methanol.TypeRef;
import org.redlance.common.utils.requester.Requester;
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

        Requester.sendRequest(request);
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

        Requester.sendRequest(request);
    }

    public static List<PostObject> requestPosts(int pages) throws IOException, InterruptedException {
        List<PostObject> postObjects = new ArrayList<>();

        // popular - ?sort=popular&source=all&p=1&time=7
        // all - ?sort=new&source=all&p=1&time=0

        for (int page = 1; page < pages; page++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://spworlds.ru/api/sp/posts?sort=popular&source=all&p=" + page + "&time=7"))
                    .build();

            postObjects.addAll(Requester.sendRequest(request, new TypeRef<List<PostObject>>() {}));
        }

        return postObjects;
    }

    public static void upvotePost(String postId, boolean up) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://spworlds.ru/api/sp/posts/" + postId))
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"vote\": true, \"isUpvote\": " + up + "}"))
                .build();

        Requester.sendRequest(request);
    }
}
