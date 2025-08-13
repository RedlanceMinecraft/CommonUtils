package org.redlance.common.utils.requester.mojang.obj;

import com.google.gson.annotations.SerializedName;
import org.redlance.common.utils.requester.mojang.MojangUtils;

import java.util.UUID;

@SuppressWarnings("unused")
public record BaseMojangProfile(
        @SerializedName(value = "name", alternate = "username") String name,
        @SerializedName(value = "id", alternate = {"uuid", "minecraftUUID"}) String id,
        @SerializedName(value = "errorMessage", alternate = "error") String errorMessage
) {
    public BaseMojangProfile(String name, String id) {
        this(name, id, null);
    }

    public BaseMojangProfile(String name, UUID id) {
        this(name, MojangUtils.toString(id));
    }

    public UUID uuid() {
        if (MojangUtils.MOJANG_BROKEN_UUID_LENGTH == id().length()) {
            return MojangUtils.parseUuid(id());
        }
        return UUID.fromString(id());
    }
}
