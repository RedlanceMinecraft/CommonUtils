package org.redlance.common.emotecraft.serializer.entries;

import io.github.kosmx.emotes.common.SerializableConfig;

import java.util.List;

public class ListConfigEntry<T> extends SerializableConfig.ConfigEntry<List<T>> {
    public ListConfigEntry(String name, List<T> defVal, boolean hasTooltip, List<SerializableConfig.ConfigEntry<?>> collection, boolean hidden) {
        super(name, null, defVal, hasTooltip, collection, hidden);
    }

    public ListConfigEntry(String name, List<T> defVal, boolean hasTooltip, List<SerializableConfig.ConfigEntry<?>> collection) {
        super(name, defVal, hasTooltip, collection);
    }

    public ListConfigEntry(String name, List<T> defVal, List<SerializableConfig.ConfigEntry<?>> collection, boolean hidden) {
        super(name, defVal, collection, hidden);
    }
}
