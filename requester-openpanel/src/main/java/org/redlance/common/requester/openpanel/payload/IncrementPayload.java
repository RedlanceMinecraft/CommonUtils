package org.redlance.common.requester.openpanel.payload;

import java.util.Objects;

/**
 * Payload of an OpenPanel {@code increment} event.
 */
public record IncrementPayload(String profileId, String property, long value) {
    public IncrementPayload {
        if (Objects.requireNonNull(profileId, "profileId").isBlank()) {
            throw new IllegalArgumentException("profileId must not be blank");
        }
        if (Objects.requireNonNull(property, "property").isBlank()) {
            throw new IllegalArgumentException("property must not be blank");
        }
    }
}
