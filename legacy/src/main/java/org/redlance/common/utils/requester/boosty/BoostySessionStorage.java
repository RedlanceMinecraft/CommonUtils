package org.redlance.common.utils.requester.boosty;

import tools.jackson.databind.node.ObjectNode;
import org.redlance.common.CommonUtils;
import org.redlance.common.utils.cache.BaseCache;
import org.redlance.common.utils.requester.Requester;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class BoostySessionStorage {
    private static final ScheduledExecutorService TOKEN_REFRESHER = CommonUtils.createScheduledExecutor(1, "token-refresher-");

    private final BaseCache<Storage> storage;
    private final String deviceId;

    public BoostySessionStorage(Path root, String deviceId) {
        this.storage = new BaseCache<>(root.resolve(String.format("boosty-%s.json", deviceId)), CommonUtils.OBJECT_MAPPER,
                () -> new Storage(null, null, 0L), CommonUtils.constructType(Storage.class)
        );

        this.deviceId = deviceId;
        this.refreshTokens();

        BoostySessionStorage.TOKEN_REFRESHER.scheduleAtFixedRate(
                this::refreshTokens, 1L, 1L, TimeUnit.DAYS
        );
    }

    public void refreshTokens() {
        CommonUtils.LOGGER.info("Refreshing boosty tokens for device {}...", this.deviceId);

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

            CommonUtils.LOGGER.debug("Refreshed boosty tokens for device {}!", this.deviceId);
        } catch (Throwable th) {
            CommonUtils.LOGGER.error("Failed to refresh boosty tokens for device {}!", this.deviceId, th);
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
