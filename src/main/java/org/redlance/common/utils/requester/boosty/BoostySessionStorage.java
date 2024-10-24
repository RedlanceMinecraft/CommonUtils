package org.redlance.common.utils.requester.boosty;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.redlance.common.CommonUtils;
import org.redlance.common.utils.cache.BaseCache;
import org.redlance.common.utils.requester.Requester;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class BoostySessionStorage {
    private final BaseCache<Storage> storage;
    private final String deviceId;

    public BoostySessionStorage(String deviceId) {
        this.storage = new BaseCache<>(String.format("boosty-%s.json", deviceId),
                () -> new Storage(null, null, 0L), TypeToken.get(Storage.class)
        );

        this.deviceId = deviceId;
        this.refreshTokens();

        CommonUtils.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                this::refreshTokens, 1L, 1L, TimeUnit.DAYS
        );
    }

    public void refreshTokens() {
        CommonUtils.LOGGER.info("Refreshing boosty tokens for device {}...", this.deviceId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.boosty.to/oauth/token/"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                        String.format("device_id=%s&device_os=web&grant_type=refresh_token&refresh_token=%s", this.deviceId, getRefreshToken())
                ))
                .build();

        this.storage.setObj(Requester.sendRequestAsync(request, JsonObject.class)
                .whenComplete((object, throwable) -> {
                    if (!object.has("access_token") && throwable == null) {
                        throw new NullPointerException(object.toString());
                    }
                })
                .thenApply(Storage::new)
                .whenComplete((storage, throwable) -> {
                    if (throwable == null) {
                        CommonUtils.LOGGER.debug("Refreshed boosty tokens for device {}!", this.deviceId);
                    } else {
                        CommonUtils.LOGGER.error("Failed to refresh boosty tokens for device {}!", this.deviceId, throwable);
                    }
                })
        ).join();
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
        public Storage(JsonObject object) {
            this(
                    object.get("access_token").getAsString(),
                    object.get("refresh_token").getAsString(),
                    Instant.now().getEpochSecond() + object.get("expires_in").getAsLong()
            );
        }
    }
}
