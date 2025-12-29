package org.redlance.common.requester.mojang.obj;

import com.fasterxml.jackson.annotation.JsonAlias;
import org.redlance.common.requester.mojang.MojangUtils;

import java.util.UUID;

@SuppressWarnings("unused")
public record BaseMojangProfile(
        @JsonAlias(value = "username") String name,
        @JsonAlias(value = { "uuid", "minecraftUUID"}) String id,
        @JsonAlias(value = "error") String errorMessage
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
