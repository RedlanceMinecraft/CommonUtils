package org.redlance.common.requester.openpanel.payload;

import org.jetbrains.annotations.Nullable;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.Objects;

/**
 * Payload of an OpenPanel {@code track} event.
 */
public record TrackPayload(String name, @Nullable String profileId, ObjectNode properties) {
    public TrackPayload {
        if (Objects.requireNonNull(name, "name").isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        properties = Objects.requireNonNull(properties, "properties").deepCopy();
    }

    public TrackPayload(String name) {
        this(name, null, JsonNodeFactory.instance.objectNode());
    }

    public TrackPayload(String name, ObjectNode properties) {
        this(name, null, properties);
    }

    public TrackPayload(String name, @Nullable String profileId) {
        this(name, profileId, JsonNodeFactory.instance.objectNode());
    }
}
