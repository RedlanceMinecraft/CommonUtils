package org.redlance.common.utils.requester.boosty.handler;

import org.jetbrains.annotations.Nullable;
import org.redlance.common.CommonUtils;
import tools.jackson.databind.node.ObjectNode;

import java.util.Objects;

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
        handle(channel, object, CommonUtils.OBJECT_MAPPER.convertValue(
                object.get("data").get("data"),
                Objects.requireNonNull(parseObject())
        ));
    }

    @Nullable Class<T> parseObject();
}
