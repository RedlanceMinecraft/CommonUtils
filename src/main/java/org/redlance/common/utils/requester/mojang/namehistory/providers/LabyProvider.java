/*package org.redlance.common.utils.requester.mojang.namehistory.providers;

import com.github.mizosoft.methanol.MutableRequest;
import org.redlance.common.utils.requester.Requester;
import org.redlance.common.utils.requester.mojang.MojangRequester;
import org.redlance.common.utils.requester.mojang.namehistory.INameProvider;
import org.redlance.common.utils.requester.mojang.namehistory.obj.Username;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Collections;
import java.util.List;

/**
 * Uses <a href="https://laby.net/">laby.net</a> to get nickname changes.
 * More intended for users of labymod itself...
 * Thank them so much!
 */
/*public class LabyProvider implements INameProvider {
    @Override
    public List<Username> getNameHistoryByName(String name) throws IOException, InterruptedException {
        return getNameHistoryById(MojangRequester.getIdByName(name));
    }

    @Override
    public List<Username> getNameHistoryById(String uuid) throws IOException, InterruptedException {
        HttpRequest request = MutableRequest.create()
                .uri(URI.create("https://laby.net/api/v3/user/" + uuid + "/profile"))
                .cacheControl(MojangRequester.CACHE_CONTROL)
                .build();

        Response response = Requester.sendRequest(request, Response.class);
        if (response.usernames() == null) {
            return Collections.emptyList();
        }

        return response.usernames();
    }
}*/
