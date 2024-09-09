package org.redlance.common.emotecraft.serializer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.ToNumberPolicy;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import io.github.kosmx.emotes.common.SerializableConfig;
import io.github.kosmx.emotes.server.config.Serializer;
import org.redlance.common.CommonUtils;

import java.io.BufferedReader;
import java.util.function.Supplier;

public class RedlanceSerializer<T extends SerializableConfig> extends Serializer {
    protected final Supplier<T> configSuppler;
    protected final Class<T> configClass;

    public RedlanceSerializer(Supplier<T> configSuppler, Class<T> configClass) {
        this.configSuppler = configSuppler;
        this.configClass = configClass;

        initializeSerializer();
    }

    @Override
    public void initializeSerializer() {
        if (this.configClass == null) {
            CommonUtils.LOGGER.warn("EmoteCraft shit really");
            return;
        }

        super.initializeSerializer();
    }

    @Override
    protected void registerTypeAdapters(GsonBuilder builder) {
        super.registerTypeAdapters(builder);

        builder.setObjectToNumberStrategy(ToNumberPolicy.LAZILY_PARSED_NUMBER);

        builder.registerTypeAdapter(this.configClass, new RedlanceConfigSerializer<>(this.configSuppler));
        builder.registerTypeAdapter(KeyframeAnimation.class, FastAnimationSerializer.INSTANCE);
    }

    @Override
    protected T readConfig(BufferedReader reader) throws JsonSyntaxException, JsonIOException {
        if (reader != null) {
            T config = serializer.fromJson(reader, this.configClass);

            if (config == null) {
                throw new JsonParseException("Json is empty");
            } else {
                return config;
            }
        } else {
            return this.configSuppler.get();
        }
    }
}
