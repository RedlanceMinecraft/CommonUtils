package org.redlance.common.utils.requester.mojang;

import com.github.mizosoft.methanol.CacheControl;
import com.github.mizosoft.methanol.MutableRequest;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.kosmx.emotes.server.config.Serializer;
import org.redlance.common.CommonUtils;
import org.redlance.common.utils.requester.Requester;
import org.redlance.common.utils.requester.mojang.obj.BaseMojangProfile;
import org.redlance.common.utils.requester.mojang.obj.MojangProfile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Optional;

@SuppressWarnings("unused")
public class MojangRequester {
    public static final CacheControl CACHE_CONTROL = CacheControl.newBuilder()
            .maxAge(Duration.ofDays(1))
            .build();

    public static BaseMojangProfile getBaseByName(String name) throws IOException, InterruptedException {
        HttpRequest request = MutableRequest.create()
                .uri(URI.create("https://api.minecraftservices.com/minecraft/profile/lookup/name/" + name))
                .cacheControl(MojangRequester.CACHE_CONTROL)
                .build();

        BaseMojangProfile profile = Requester.sendRequest(request, BaseMojangProfile.class);
        if (profile.errorMessage() != null) {
            throw new InterruptedException(profile.errorMessage());
        }

        return profile;
    }

    public static String getIdByName(String name) throws IOException, InterruptedException {
        BaseMojangProfile response = getBaseByName(name.trim());
        if (response.id() == null) return null;
        return MojangUtils.toString(response.uuid());
    }

    public static Optional<MojangProfile> getMojangProfileByName(String name) {
        try {
            BaseMojangProfile response = getBaseByName(name.trim());
            if (response.id() == null) return Optional.empty();
            return getMojangProfileById(response.id());
        } catch (Throwable throwable) {
            CommonUtils.LOGGER.warn("Failed to send request!", throwable);
            return Optional.empty();
        }
    }

    public static Optional<MojangProfile> getMojangProfileById(String uuid) throws IOException, InterruptedException {
        HttpRequest request = MutableRequest.create()
                .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid))
                .cacheControl(MojangRequester.CACHE_CONTROL)
                .build();

        JsonObject obj = Requester.sendRequest(request, JsonObject.class);
        if (obj == null) return Optional.empty();

        if (obj.has("errorMessage")) {
            throw new InterruptedException(obj.get("errorMessage").getAsString());
        }

        for (JsonElement elem : obj.getAsJsonArray("properties")) {
            JsonObject jsonObject = elem.getAsJsonObject();

            if (!jsonObject.has("name") || !"textures".equals(jsonObject.get("name").getAsString())) {
                continue;
            }

            try (Reader reader = new InputStreamReader(new ByteArrayInputStream(
                    Base64.getDecoder().decode(jsonObject.get("value").getAsString())
            ))) {
                return Optional.of(Serializer.getSerializer().fromJson(reader, MojangProfile.class));
            }
        }

        if (obj.has("id") && obj.has("name")) {
            return Optional.of(new MojangProfile(-1,
                    obj.get("id").getAsString(), obj.get("name").getAsString(), new HashMap<>(0)
            ));
        }

        return Optional.empty();
    }
}
