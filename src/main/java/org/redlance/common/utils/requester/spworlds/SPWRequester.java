package org.redlance.common.utils.requester.spworlds;

import com.github.mizosoft.methanol.MediaType;
import com.github.mizosoft.methanol.MoreBodyPublishers;
import com.github.mizosoft.methanol.MultipartBodyPublisher;
import com.google.gson.reflect.TypeToken;
import org.redlance.common.utils.requester.Requester;
import org.redlance.common.utils.requester.spworlds.obj.PostObject;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class SPWRequester {
    public static void authDiscord(String code) throws ExecutionException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://spworlds.ru/api/auth/discord"))
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"code\":\"" + code + "\"}"))
                .build();

        Requester.sendRequest(request);
    }

    public static void createPost(String groupId, String text, byte[] image) throws ExecutionException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://spworlds.ru/api/sp/posts/image?groupId=" + groupId))
                .header("content-type", "multipart/form-data")
                .POST(MultipartBodyPublisher.newBuilder()
                        .textPart("text", text)
                        .formPart("image", text + ".png",
                                MoreBodyPublishers.ofMediaType(
                                        HttpRequest.BodyPublishers.ofByteArray(image), MediaType.IMAGE_PNG
                                )
                        )
                        .build()
                )
                .build();

        Requester.sendRequest(request);
    }

    @SuppressWarnings("unchecked")
    public static List<PostObject> requestPosts(int pages) throws ExecutionException {
        List<PostObject> postObjects = new ArrayList<>();

        // popular - ?sort=popular&source=all&p=1&time=7
        // all - ?sort=new&source=all&p=1&time=0

        for (int page = 1; page < pages; page++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://spworlds.ru/api/sp/posts?sort=popular&source=all&p=" + page + "&time=7"))
                    .build();

            postObjects.addAll((java.util.Collection<? extends PostObject>)
                    Requester.sendRequest(request, TypeToken.getParameterized(List.class, PostObject.class))
            );
        }

        return postObjects;
    }

    public static void upvotePost(String postId, boolean up) throws ExecutionException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://spworlds.ru/api/sp/posts/" + postId))
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"vote\": true, \"isUpvote\": " + up + "}"))
                .build();

        Requester.sendRequest(request);
    }
}
