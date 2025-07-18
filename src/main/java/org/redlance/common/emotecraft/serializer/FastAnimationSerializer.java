package org.redlance.common.emotecraft.serializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.network.AnimationBinary;
import io.github.kosmx.emotes.api.proxy.AbstractNetworkInstance;
import io.github.kosmx.emotes.common.network.EmotePacket;
import io.github.kosmx.emotes.common.network.PacketConfig;
import io.github.kosmx.emotes.common.network.PacketTask;
import io.github.kosmx.emotes.common.network.objects.NetData;
import io.github.kosmx.emotes.common.tools.MathHelper;
import org.redlance.common.CommonUtils;
import org.redlance.common.emotecraft.KeyframeUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.HashMap;

public class FastAnimationSerializer implements JsonDeserializer<Animation>, JsonSerializer<Animation> {
    public static final FastAnimationSerializer INSTANCE = new FastAnimationSerializer();

    private FastAnimationSerializer() {
        CommonUtils.LOGGER.debug("Serializing animations via {}!", getClass().getName());
    }

    @Override
    public Animation deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        try {
            return serialize(json.getAsString());
        } catch (Throwable e) {
            throw new JsonParseException("Failed to deserialize animation " + json + "!", e);
        }
    }

    public Animation serialize(String src) throws IOException {
        return serialize(Base64.getDecoder().decode(src));
    }

    public Animation serialize(byte[] src) throws IOException {
        try (InputStream is = new ByteArrayInputStream(src)) {
            NetData data = new EmotePacket.Builder()
                    .strictSizeLimit(false)
                    .build()
                    .read(MathHelper.readFromIStream(is));

            if (data.purpose != PacketTask.FILE || data.emoteData == null) {
                throw new IllegalStateException("Binary emote is invalid!");
            }
            return data.emoteData;
        }
    }

    @Override
    public JsonElement serialize(Animation src, Type type, JsonSerializationContext context) {
        try {
            return context.serialize(serializeToString(src), String.class);
        } catch (Throwable e) {
            CommonUtils.LOGGER.error("Failed to serialize animation {}!", src, e);
            return JsonNull.INSTANCE;
        }
    }

    public String serializeToString(Animation src) throws IOException {
        return Base64.getEncoder().encodeToString(serializeToBytes(src));
    }

    public byte[] serializeToBytes(Animation src) throws IOException {
        return AbstractNetworkInstance.safeGetBytesFromBuffer(serializeToByteBuff(src));
    }

    public ByteBuffer serializeToByteBuff(Animation src) throws IOException {
        return new EmotePacket.Builder().configureToSaveEmote(src).build(Integer.MAX_VALUE, false).write();
    }

    public static HashMap<Byte, Byte> getDowngradedHashMap(Animation animation) {
        HashMap<Byte, Byte> version = new HashMap<>();

        // Animation packet
        if (animation.data().isAnimationPlayerAnimatorFormat()) {
            if (KeyframeUtils.hasEasingArgs(animation)) {
                version.put(PacketConfig.LEGACY_ANIMATION_FORMAT, (byte) 4);
            } else if (KeyframeUtils.hasScaling(animation)) {
                version.put(PacketConfig.LEGACY_ANIMATION_FORMAT, (byte) 3);
            } else if (KeyframeUtils.hasDynamicParts(animation)) {
                version.put(PacketConfig.LEGACY_ANIMATION_FORMAT, (byte) 2);
            } else {
                version.put(PacketConfig.LEGACY_ANIMATION_FORMAT, (byte) 1);
            }
            version.remove(PacketConfig.NEW_ANIMATION_FORMAT);
        } else {
            version.put(PacketConfig.NEW_ANIMATION_FORMAT, (byte) AnimationBinary.CURRENT_VERSION);
            version.remove(PacketConfig.NEW_ANIMATION_FORMAT);
        }

        // Header packet
        if (animation.data().has("bages")) {
            version.put(PacketConfig.HEADER_PACKET, (byte) 2);
        } else {
            version.put(PacketConfig.HEADER_PACKET, (byte) 1);
        }

        return version;
    }
}
