package org.redlance.common.utils.requester.boosty.obj.user;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jetbrains.annotations.NotNull;

public record BoostyPromo(ObjectNode access, ObjectNode count, String description, Object endTime, int id, boolean isFinished, String linkId, long startTime, ObjectNode trial, String type) {
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
