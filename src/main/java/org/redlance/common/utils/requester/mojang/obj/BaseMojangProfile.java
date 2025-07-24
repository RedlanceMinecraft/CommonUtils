package org.redlance.common.utils.requester.mojang.obj;

import com.google.gson.annotations.SerializedName;
import org.redlance.common.utils.requester.mojang.MojangUtils;

import java.util.UUID;

public record BaseMojangProfile(
        @SerializedName(value = "name", alternate = "username") String name,
        @SerializedName(value = "id", alternate = {"uuid", "minecraftUUID"}) String id,
        @SerializedName(value = "errorMessage", alternate = "error") String errorMessage
) {
    public UUID uuid() {
        if (MojangUtils.MOJANG_BROKEN_UUID_LENGTH == id().length()) {
            return MojangUtils.parseUuid(id());
        }
        return UUID.fromString(id());
    }
}
