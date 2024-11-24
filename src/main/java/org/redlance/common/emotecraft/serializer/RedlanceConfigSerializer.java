package org.redlance.common.emotecraft.serializer;

import io.github.kosmx.emotes.common.SerializableConfig;
import io.github.kosmx.emotes.server.config.ConfigSerializer;

import java.util.function.Supplier;

public class RedlanceConfigSerializer<T extends SerializableConfig> extends ConfigSerializer {
    protected final Supplier<T> configSuppler;

    public RedlanceConfigSerializer(Supplier<T> configSuppler) {
        this.configSuppler = configSuppler;
    }

    @Override
    protected SerializableConfig newConfig() {
        return this.configSuppler.get();
    }
}
