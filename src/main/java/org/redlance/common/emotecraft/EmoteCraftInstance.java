package org.redlance.common.emotecraft;

import io.github.kosmx.emotes.common.SerializableConfig;
import io.github.kosmx.emotes.server.config.ConfigSerializer;
import io.github.kosmx.emotes.server.config.Serializer;
import org.redlance.common.CommonUtils;

import java.util.function.Supplier;

public class EmoteCraftInstance {
    /*public static <T extends SerializableConfig> void tryInitializeInstance(Supplier<T> configSuppler, Class<T> configClass) {
        EmoteCraftInstance.tryInitializeInstance(configSuppler, configClass, null);
    }*/

    public static <T extends SerializableConfig> T tryInitializeInstance(Supplier<T> configSuppler, Class<T> configClass) {
        if (Serializer.INSTANCE != null) {
            CommonUtils.LOGGER.warn("Emotecraft is loaded multiple times, please load it only once!");
            return getConfigAs();
        }

        Serializer.INSTANCE = new Serializer<>(new ConfigSerializer<>(configSuppler), configClass)/* {
            @Override
            protected Gson initializeSerializer(GsonBuilder builder) {
                builder.registerTypeAdapter(Animation.class, FastAnimationSerializer.INSTANCE);
                builder.registerTypeAdapterFactory(new CompletableFutureAdapterFactory());
                return super.initializeSerializer(builder);
            }
        }*/;

        return getConfigAs();
    }

    @SuppressWarnings("unchecked")
    public static <T extends SerializableConfig> T getConfigAs() {
        if (Serializer.INSTANCE == null) {
            throw new NullPointerException("Serializer not initialized!");
        }

        return (T) Serializer.getConfig();
    };
}
