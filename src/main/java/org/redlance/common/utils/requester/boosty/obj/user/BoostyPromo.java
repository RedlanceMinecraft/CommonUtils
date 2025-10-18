package org.redlance.common.utils.requester.boosty.obj.user;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

public record BoostyPromo(JsonObject access, JsonObject count, String description, Object endTime, int id, boolean isFinished, String linkId, long startTime, JsonObject trial, String type) {
    @Override
    public @NotNull String toString() {
        return String.format("BoostyPromo{%s (%s)}", linkId, type);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof BoostyPromo boostyUser) {
            return this.id == boostyUser.id;
        }

        if (other instanceof Number in) {
            return this.id == in.intValue();
        }

        return false;
    }
}
