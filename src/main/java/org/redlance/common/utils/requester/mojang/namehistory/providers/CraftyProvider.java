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
 * Uses <a href="https://crafty.gg/">crafty.gg</a> to get nickname changes.
 * Thank them so much!
 */
/*public class CraftyProvider implements INameProvider {
    @Override
    public List<Username> getNameHistoryByName(String name) throws IOException, InterruptedException {
        HttpRequest request = MutableRequest.create()
                .uri(URI.create("https://crafty.gg/players/" + name + ".json"))
                .cacheControl(MojangRequester.CACHE_CONTROL)
                .build();

        Response response = Requester.sendRequest(request, Response.class);
        if (response.usernames() == null) {
            return Collections.emptyList();
        }

        return response.usernames();
    }

    @Override
    public List<Username> getNameHistoryById(String uuid) throws IOException, InterruptedException {
        return getNameHistoryByName(uuid);
    }
}*/
