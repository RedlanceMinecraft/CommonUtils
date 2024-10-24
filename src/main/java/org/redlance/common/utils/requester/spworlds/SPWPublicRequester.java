package org.redlance.common.utils.requester.spworlds;

import com.github.mizosoft.methanol.MutableRequest;
import org.redlance.common.utils.LambdaExceptionUtils;
import org.redlance.common.utils.requester.Requester;
import org.redlance.common.utils.requester.mojang.MojangRequester;
import org.redlance.common.utils.requester.mojang.obj.BaseMojangProfile;
import org.redlance.common.utils.requester.mojang.obj.MojangProfile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class SPWPublicRequester {
    public static Optional<MojangProfile> getMojangProfile(List<String> cards, long discordId) {
        final List<CompletableFuture<Optional<MojangProfile>>> futures = new ArrayList<>();

        for (String card : cards) {
            futures.add(CompletableFuture.supplyAsync(LambdaExceptionUtils.rethrowSupplier(
                    () -> getMojangProfile(card, discordId)
            )));
        }

        return futures.parallelStream()
                .map(CompletableFuture::join)
                .map(opt -> opt.orElse(null))
                .filter(Objects::nonNull)
                .findFirst();
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
        if (response.id == null) {
            return Optional.empty();
        }

        return MojangRequester.getMojangProfileById(response.id);
    }
}
