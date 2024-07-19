package org.redlance.common.utils.requester.boosty.handler;

import com.google.gson.JsonObject;
import io.github.kosmx.emotes.server.config.Serializer;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public interface BoostyListener<T> {
    default void handle(T type) {
        throw new NotImplementedException();
    }

    default void handle(JsonObject object, T type) {
        handle(type);
    }

    default void handle(JsonObject object) {
        handle(object, Serializer.serializer.fromJson(
                object.getAsJsonObject("data").getAsJsonObject("data"),
                Objects.requireNonNull(parseObject())
        ));
    }

    @Nullable Class<T> parseObject();
}
