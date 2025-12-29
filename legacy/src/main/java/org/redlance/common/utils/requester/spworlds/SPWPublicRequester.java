package org.redlance.common.utils.requester.spworlds;

import com.github.mizosoft.methanol.MutableRequest;
import org.redlance.common.utils.requester.Requester;
import org.redlance.common.utils.requester.mojang.MojangRequester;
import org.redlance.common.utils.requester.mojang.obj.BaseMojangProfile;
import org.redlance.common.utils.requester.mojang.obj.MojangProfile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
public class SPWPublicRequester {
    public static Optional<MojangProfile> getMojangProfile(List<String> cards, long discordId) {
        return Requester.prepareParallelRequests(cards.stream(), card -> {
            try {
                return getMojangProfile(card, discordId).orElse(null);
            } catch (Throwable th) {
                CommonUtils.LOGGER.warn("Failed to get profile with card {}!", card, th);
                return null;
            }
        }).filter(Objects::nonNull).findFirst();
    }

    public static Optional<MojangProfile> getMojangProfile(String card, long discordId) throws IOException, InterruptedException {
        HttpRequest request = MutableRequest.create()
                .uri(URI.create("https://spworlds.ru/api/public/users/" + discordId))
                .header("Authorization", "Bearer " + Base64.getEncoder().encodeToString(card
                        .getBytes(StandardCharsets.UTF_8)
                ))
                .cacheControl(MojangRequester.CACHE_CONTROL)
                .build();

        BaseMojangProfile response = Requester.sendRequest(request, BaseMojangProfile.class);
        if (response.id() == null) return Optional.empty();
        return MojangRequester.getMojangProfileById(response.id());
    }
}
