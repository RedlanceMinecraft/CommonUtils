package org.redlance.common.emotecraft;

import io.github.kosmx.emotes.server.config.CommonConfig;
import io.github.kosmx.emotes.server.config.ConfigSerializer;
import io.github.kosmx.emotes.server.config.Serializer;
import io.github.kosmx.emotes.server.services.InstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.function.Supplier;

public class EmoteCraftInstance {
    private static final Logger LOGGER = LoggerFactory.getLogger("EmoteCraftInstance");

    public static <T extends CommonConfig> void tryInitializeInstance(Supplier<T> configSuppler, int version, Class<T> configClass) {
        EmoteCraftInstance.tryInitializeInstance(configSuppler, version, configClass, InstanceService.INSTANCE.getConfigPath());
    }

    public static <T extends CommonConfig> T tryInitializeInstance(Supplier<T> configSuppler, int version, Class<T> configClass, Path configPath) {
        if (Serializer.INSTANCE != null) {
            EmoteCraftInstance.LOGGER.warn("Emotecraft is loaded multiple times, please load it only once!");
            return getConfigAs();
        }

        Serializer.INSTANCE = new Serializer<>(new ConfigSerializer<>(configSuppler, version), configClass, configPath);
        return getConfigAs();
    }

    @SuppressWarnings("unchecked")
    public static <T extends CommonConfig> T getConfigAs() {
        if (Serializer.INSTANCE == null) {
            throw new NullPointerException("Serializer not initialized!");
        }

        return (T) Serializer.getConfig();
    };
}
