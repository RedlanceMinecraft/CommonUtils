package org.redlance.common.utils.requester.boosty.handler;

import com.google.gson.JsonObject;
import io.github.kosmx.emotes.server.config.Serializer;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public interface BoostyListener<T> {
    default String getName() {
        return getClass().getName();
    }

    default void handle(String channel, T type) {
        throw new NotImplementedException();
    }

    default void handle(String channel, JsonObject object, T type) {
        handle(channel, type);
    }

    default void handle(String channel, JsonObject object) {
        handle(channel, object, Serializer.serializer.fromJson(
                object.getAsJsonObject("data").getAsJsonObject("data"),
                Objects.requireNonNull(parseObject())
        ));
    }

    @Nullable Class<T> parseObject();
}
