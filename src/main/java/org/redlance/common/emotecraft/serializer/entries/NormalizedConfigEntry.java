package org.redlance.common.emotecraft.serializer.entries;

import io.github.kosmx.emotes.common.SerializableConfig;

import java.util.List;

public class NormalizedConfigEntry<T> extends SerializableConfig.ConfigEntry<T> {
    public NormalizedConfigEntry(String name, T defVal, boolean hasTooltip, List<SerializableConfig.ConfigEntry<?>> collection, boolean hidden) {
        super(name, null, defVal, hasTooltip, collection, hidden);
    }

    public NormalizedConfigEntry(String name, T defVal, boolean hasTooltip, List<SerializableConfig.ConfigEntry<?>> collection) {
        super(name, defVal, hasTooltip, collection);
    }

    public NormalizedConfigEntry(String name, T defVal, List<SerializableConfig.ConfigEntry<?>> collection, boolean hidden) {
        super(name, defVal, collection, hidden);
    }
}
