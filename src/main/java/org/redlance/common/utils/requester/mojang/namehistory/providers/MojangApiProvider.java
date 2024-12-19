package org.redlance.common.utils.requester.mojang.namehistory.providers;

import com.github.mizosoft.methanol.MutableRequest;
import com.github.mizosoft.methanol.TypeRef;
import org.redlance.common.utils.requester.Requester;
import org.redlance.common.utils.requester.mojang.MojangRequester;
import org.redlance.common.utils.requester.mojang.namehistory.INameProvider;
import org.redlance.common.utils.requester.mojang.namehistory.obj.Username;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.List;

/**
 * Implemented provider for the official api (which is deprecated)
 * It is used in most cases for its simulations
 */
public class MojangApiProvider implements INameProvider {
    protected final String endpoint;

    public MojangApiProvider(String endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public List<Username> getNameHistoryByName(String name) throws IOException, InterruptedException {
        return getNameHistoryById(MojangRequester.getIdByName(this.endpoint, name), true);
    }

    @Override
    public List<Username> getNameHistoryById(String uuid) throws IOException, InterruptedException {
        return getNameHistoryById(uuid, false);
    }

    public List<Username> getNameHistoryById(String uuid, boolean converted) throws IOException, InterruptedException {
        HttpRequest request = MutableRequest.create()
                .uri(URI.create("https://" + this.endpoint + "/user/profiles/" + uuid + "/names"))
                .cacheControl(MojangRequester.CACHE_CONTROL)
                .build();

        return Requester.sendRequest(request, new TypeRef<>() {});
    }
}
