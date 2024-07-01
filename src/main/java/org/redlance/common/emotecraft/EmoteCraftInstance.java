package org.redlance.common.emotecraft;

import io.github.kosmx.emotes.common.CommonData;
import io.github.kosmx.emotes.common.SerializableConfig;
import io.github.kosmx.emotes.executor.EmoteInstance;
import io.github.kosmx.emotes.server.config.Serializer;
import org.redlance.common.CommonUtils;
import org.redlance.common.emotecraft.serializer.RedlanceSerializer;
import org.redlance.common.emotecraft.utils.EmoteInstanceImpl;

import java.util.function.Supplier;

public class EmoteCraftInstance {
    public static <T extends SerializableConfig> T tryInitializeInstance(Supplier<T> configSuppler, Class<T> configClass) {
        if (CommonData.isLoaded) {
            CommonUtils.LOGGER.warn("Emotecraft is loaded multiple times, please load it only once!");
            return getConfigAs();
        }

        CommonData.isLoaded = true;
        if (EmoteInstance.instance == null) { // Custom instance
            EmoteInstance.instance = new EmoteInstanceImpl();
        }
        Serializer.INSTANCE = new RedlanceSerializer<>(configSuppler, configClass);

        return getConfigAs();
    }

    @SuppressWarnings("unchecked")
    public static <T extends SerializableConfig> T getConfigAs() {
        if (Serializer.serializer == null) {
            throw new NullPointerException("Serializer not initialized!");
        }

        return (T) Serializer.getConfig();
    }
}
