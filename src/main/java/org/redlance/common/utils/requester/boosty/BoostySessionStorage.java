package org.redlance.common.utils.requester.boosty;

import com.google.gson.JsonObject;
import org.redlance.common.CommonUtils;
import org.redlance.common.utils.CacheTemplate;
import org.redlance.common.utils.requester.Requester;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.concurrent.TimeUnit;

public class BoostySessionStorage {
    private static final String ACCESS_TOKEN = "accessToken";
    private static final String REFRESH_TOKEN = "refreshToken";
    // private static final String EXPIRES_AT = "expiresAt";

    private final CacheTemplate<String, String> storage;
    private final String deviceId;

    public BoostySessionStorage(String deviceId) {
        this.storage = new CacheTemplate<>(
                String.format("boosty-%s.json", deviceId), String.class, String.class
        );

        this.deviceId = deviceId;

        CommonUtils.EXECUTOR.scheduleAtFixedRate(
                this::refreshTokens, 0L, 1L, TimeUnit.DAYS
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

        try {
            JsonObject object = Requester.sendRequest(request, JsonObject.class);
            if (!object.has("access_token")) {
                throw new NullPointerException(object.toString());
            }

            this.storage.write(ACCESS_TOKEN, object.get("access_token").getAsString(), true);
            this.storage.write(REFRESH_TOKEN, object.get("refresh_token").getAsString(), true);

            /*this.storage.write(EXPIRES_AT, Long.toString(
                    Instant.now().getEpochSecond() + object.get("expires_in").getAsLong()
            ), true);*/

        } catch (Throwable th) {
            CommonUtils.LOGGER.error("Failed to refresh boosty tokens for device {}!", this.deviceId, th);
        }

        CommonUtils.LOGGER.debug("Refreshed boosty tokens for device {}!", this.deviceId);
    }

    public String getAccessToken() {
        return this.storage.getValueByKey(ACCESS_TOKEN);
    }

    protected String getRefreshToken() {
        return this.storage.getValueByKey(REFRESH_TOKEN);
    }

    /*public Long getExpiresAt() {
        return Long.parseLong(this.storage.getValueByKey(EXPIRES_AT));
    }*/
}
