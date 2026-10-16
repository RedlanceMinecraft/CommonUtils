package org.redlance.common.requester.boosty.handler;

import org.jetbrains.annotations.Nullable;
import org.redlance.common.jackson.JacksonMappers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.Objects;

/**
 * Listens to one type of websocket event, see {@link BoostyBuiltInHandler}.
 * <p>
 * {@link #handle(String, ObjectNode)} reads the {@link #payload(ObjectNode)} of the event into {@link #parseObject()}.
 *
 * @param <T> Type of the payload.
 */
@SuppressWarnings("unused")
public interface BoostyListener<T> {
    default String getName() {
        return getClass().getName();
    }

    default void handle(String channel, T type) {
        throw new UnsupportedOperationException();
    }

    default void handle(String channel, ObjectNode object, T type) {
        handle(channel, type);
    }

    default void handle(String channel, ObjectNode object) {
        handle(channel, object, JacksonMappers.OBJECT_MAPPER.convertValue(
                payload(object),
                Objects.requireNonNull(parseObject())
        ));
    }

    @Nullable Class<T> parseObject();

    /**
     * Payload of an event, {@code {"data": {"type": ..., ...}}}. Boosty nests it as {@code data.data} for most events
     * and puts it next to {@code type} for the rest ({@code WEBSOCKET.md} in this module tells which), so this is
     * {@code data.data} when the event has one and {@code data} itself, {@code type} included, otherwise.
     *
     * @return {@code null} for an event without {@code data}.
     */
    static @Nullable JsonNode payload(ObjectNode event) {
        JsonNode published = event.get("data");
        if (published == null || published.isNull()) {
            return null;
        }

        JsonNode nested = published.get("data");
        return nested == null || nested.isNull() ? published : nested;
    }
}
