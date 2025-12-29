package org.redlance.common.requester.boosty;

import org.redlance.common.cache.BaseCache;
import org.redlance.common.jackson.JacksonMappers;
import org.redlance.common.requester.Requester;
import org.redlance.common.requester.RequesterUtils;
import org.redlance.common.utils.CommonExecutors;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class BoostySessionStorage {
    private static final ScheduledExecutorService TOKEN_REFRESHER = CommonExecutors.createScheduledExecutor(1, "token-refresher-");

    private final BaseCache<Storage> storage;
    private final String deviceId;

    public BoostySessionStorage(Path root, String deviceId) {
        this.storage = new BaseCache<>(root.resolve(String.format("boosty-%s.json", deviceId)), JacksonMappers.OBJECT_MAPPER,
                () -> new Storage(null, null, 0L), JacksonMappers.constructType(Storage.class)
        );

        this.deviceId = deviceId;
        this.refreshTokens();

        BoostySessionStorage.TOKEN_REFRESHER.scheduleAtFixedRate(
                this::refreshTokens, 1L, 1L, TimeUnit.DAYS
        );
    }

    public void refreshTokens() {
        RequesterUtils.LOGGER.info("Refreshing boosty tokens for device {}...", this.deviceId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.boosty.to/oauth/token/"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Bearer " + getAccessToken())
                .POST(HttpRequest.BodyPublishers.ofString(
                        String.format("device_id=%s&device_os=web&grant_type=refresh_token&refresh_token=%s", this.deviceId, getRefreshToken())
                ))
                .build();

        try {
            ObjectNode object = Requester.sendRequest(request, ObjectNode.class);
            if (!object.has("access_token")) {
                throw new NullPointerException(object.toString());
            }

            this.storage.setObj(new Storage(object));

            RequesterUtils.LOGGER.debug("Refreshed boosty tokens for device {}!", this.deviceId);
        } catch (Throwable th) {
            RequesterUtils.LOGGER.error("Failed to refresh boosty tokens for device {}!", this.deviceId, th);
        }
    }

    public String getAccessToken() {
        return this.storage.getObj().accessToken;
    }

    public String getRefreshToken() {
        return this.storage.getObj().refreshToken;
    }

    public long getExpiresAt() {
        return this.storage.getObj().expiresAt;
    }

    private record Storage(String accessToken, String refreshToken, long expiresAt) {
        public Storage(ObjectNode object) {
            this(
                    object.get("access_token").asString(),
                    object.get("refresh_token").asString(),
                    Instant.now().getEpochSecond() + object.get("expires_in").asLong()
            );
        }
    }
}
