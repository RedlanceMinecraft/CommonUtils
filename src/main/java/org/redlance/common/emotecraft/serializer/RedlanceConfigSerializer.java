package org.redlance.common.emotecraft.serializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import io.github.kosmx.emotes.common.SerializableConfig;
import io.github.kosmx.emotes.server.config.ConfigSerializer;
import io.github.kosmx.emotes.server.config.Serializer;
import org.redlance.common.CommonUtils;

import java.lang.reflect.Type;
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

    @Override
    public SerializableConfig deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject node = json.getAsJsonObject();
        SerializableConfig config = newConfig();

        if (node.has("config_version")) {
            config.configVersion = node.get("config_version").getAsInt();
        } else {
            config.configVersion = SerializableConfig.staticConfigVersion;
        }

        if (config.configVersion < SerializableConfig.staticConfigVersion) {
            CommonUtils.LOGGER.warn("Serializing config {} with older version...", config);

        } else if (config.configVersion > SerializableConfig.staticConfigVersion) {
            CommonUtils.LOGGER.error("You are trying to load version {} config. The mod can only load correctly up to {}. If you won't modify any config, I won't overwrite your config file.", config.configVersion, SerializableConfig.staticConfigVersion);
        }

        config.iterate(entry -> deserializeEntry(entry, node));
        return config;
    }

    protected <T> void deserializeEntry(SerializableConfig.ConfigEntry<T> entry, JsonObject node) {
        String id = null;
        if (node.has(entry.getName())) {
            id = entry.getName();

        } else if (node.has(entry.getOldConfigName())) {
            id = entry.getOldConfigName();
        }

        if (id == null)
            return;

        JsonElement element = node.get(id);

        entry.set((T) Serializer.serializer.fromJson(element, entry.get().getClass()));
    }

    @Override
    public JsonElement serialize(SerializableConfig config, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject node = new JsonObject();
        node.addProperty("config_version", SerializableConfig.staticConfigVersion); // I always save config with the latest format.
        config.iterate(entry -> serializeEntry(entry, node));
        return node;
    }

    protected void serializeEntry(SerializableConfig.ConfigEntry<?> entry, JsonObject node) {
        node.add(entry.getName(), Serializer.serializer.toJsonTree(entry.get()));
    }
}
