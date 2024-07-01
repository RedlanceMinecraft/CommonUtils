package org.redlance.common.utils.requester.mojang;

import com.google.gson.JsonArray;
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
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class MojangRequester {
    public static BaseMojangProfile getBaseByName(String name) throws ExecutionException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + name))
                .build();

        return Requester.sendRequest(request, BaseMojangProfile.class);
    }

    public static String getIdByName(String name) throws ExecutionException {
        BaseMojangProfile response = getBaseByName(name.trim());
        if (response.id == null) {
            return null;
        }

        return MojangUtils.parseUuid(response.id);
    }

    public static Optional<MojangProfile> getMojangProfileByName(String name) {
        try {
            BaseMojangProfile response = getBaseByName(name.trim());
            if (response.id == null) {
                return Optional.empty();
            }

            return getMojangProfileById(response.id);
        } catch (Throwable throwable) {
            CommonUtils.LOGGER.warn("Failed to send request!", throwable);
        }

        return Optional.empty();
    }

    public static Optional<MojangProfile> getMojangProfileById(String uuid) throws IOException, ExecutionException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid))
                .build();

        JsonArray jsonArray = Requester.sendRequest(request, JsonObject.class)
                .getAsJsonArray("properties");

        for (JsonElement elem : jsonArray) {
            JsonObject jsonObject = elem.getAsJsonObject();

            if (!jsonObject.has("name") || !"textures".equals(jsonObject.get("name").getAsString())) {
                continue;
            }

            try (Reader reader = new InputStreamReader(new ByteArrayInputStream(
                    Base64.getDecoder().decode(jsonObject.get("value").getAsString())
            ))) {
                return Optional.of(Serializer.serializer.fromJson(reader, MojangProfile.class));
            }
        }

        return Optional.empty();
    }
}
