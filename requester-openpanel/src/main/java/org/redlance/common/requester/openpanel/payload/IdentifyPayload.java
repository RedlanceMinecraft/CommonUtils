package org.redlance.common.requester.openpanel.payload;

import org.jetbrains.annotations.Nullable;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.Objects;

/**
 * Payload of an OpenPanel {@code identify} event.
 */
public record IdentifyPayload(
        String profileId,
        @Nullable String firstName,
        @Nullable String lastName,
        @Nullable String email,
        @Nullable String avatar,
        ObjectNode properties
) {
    public IdentifyPayload {
        if (Objects.requireNonNull(profileId, "profileId").isBlank()) {
            throw new IllegalArgumentException("profileId must not be blank");
        }
        properties = Objects.requireNonNull(properties, "properties").deepCopy();
    }

    public IdentifyPayload(String profileId) {
        this(profileId, null, null, null, null, JsonNodeFactory.instance.objectNode());
    }
}
