package org.redlance.common.emotecraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import io.github.kosmx.emotes.common.CommonData;
import io.github.kosmx.emotes.common.SerializableConfig;
import io.github.kosmx.emotes.server.config.ConfigSerializer;
import io.github.kosmx.emotes.server.config.Serializer;
import org.redlance.common.CommonUtils;
import org.redlance.common.emotecraft.serializer.FastAnimationSerializer;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class EmoteCraftInstance {
    public static <T extends SerializableConfig> T tryInitializeInstance(Supplier<T> configSuppler, Class<T> configClass) {
        return EmoteCraftInstance.tryInitializeInstance(configSuppler, configClass, null);
    }

    public static <T extends SerializableConfig> T tryInitializeInstance(Supplier<T> configSuppler, Class<T> configClass, Consumer<GsonBuilder> consumer) {
        if (CommonData.isLoaded) {
            CommonUtils.LOGGER.warn("Emotecraft is loaded multiple times, please load it only once!");
            return getConfigAs();
        }

        CommonData.isLoaded = true;
        Serializer.INSTANCE = new Serializer<>(new ConfigSerializer<>(configSuppler), configClass, consumer) {
            @Override
            protected Gson initializeSerializer(GsonBuilder builder) {
                builder.registerTypeAdapter(KeyframeAnimation.class, FastAnimationSerializer.INSTANCE);
                return super.initializeSerializer(builder);
            }
        };

        return getConfigAs();
    }

    @SuppressWarnings("unchecked")
    public static <T extends SerializableConfig> T getConfigAs() {
        if (Serializer.getSerializer() == null) {
            throw new NullPointerException("Serializer not initialized!");
        }

        return (T) Serializer.getConfig();
    }
}
