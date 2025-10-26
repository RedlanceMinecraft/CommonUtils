package org.redlance.common.emotecraft.serializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.zigythebird.playeranimcore.animation.Animation;
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
    public static final FastAnimationSerializer INSTANCE = new FastAnimationSerializer(false, false);
    public static final FastAnimationSerializer DOWNGRADABLE = new FastAnimationSerializer(true, false);
    public static final FastAnimationSerializer LEGACY = new FastAnimationSerializer(true, true);

    private final boolean downgradable;
    private final boolean forcePlayerAnim;

    private FastAnimationSerializer(boolean downgradable, boolean forcePlayerAnim) {
        this.downgradable = downgradable;
        this.forcePlayerAnim = forcePlayerAnim;
        CommonUtils.LOGGER.debug("Serializing animations via {} (downgradable={}, forcePlayerAnim={})!",
                getClass().getName(), downgradable, forcePlayerAnim
        );
    }

    @Override
    public Animation deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        try {
            return serialize(json.getAsString());
        } catch (Throwable e) {
            CommonUtils.LOGGER.warn("Failed to deserialize animation {}!", json, e);
            return null;
        }
    }

    public static Animation serialize(String src) throws IOException {
        return serialize(Base64.getDecoder().decode(src));
    }

    public static Animation serialize(byte[] src) throws IOException {
        try (InputStream is = new ByteArrayInputStream(src)) {
            NetData data = new EmotePacket.Builder()
                    .strictSizeLimit(false)
                    .build()
                    .read(MathHelper.readFromIStream(is));

            if (data.purpose != PacketTask.FILE) throw new IllegalStateException("Binary emote is invalid!");
            return data.emoteData;
        }
    }

    @Override
    public JsonElement serialize(Animation src, Type type, JsonSerializationContext context) {
        try {
            return context.serialize(serializeToString(src, this.downgradable, this.forcePlayerAnim), String.class);
        } catch (Throwable e) {
            CommonUtils.LOGGER.error("Failed to serialize animation {}!", src, e);
            return JsonNull.INSTANCE;
        }
    }

    public static String serializeToString(Animation src, boolean downgradable, boolean forcePlayerAnim) throws IOException {
        return Base64.getEncoder().encodeToString(serializeToBytes(src, downgradable, forcePlayerAnim));
    }

    public static byte[] serializeToBytes(Animation src, boolean downgradable, boolean forcePlayerAnim) throws IOException {
        return AbstractNetworkInstance.safeGetBytesFromBuffer(serializeToByteBuff(src, downgradable, forcePlayerAnim));
    }

    public static ByteBuffer serializeToByteBuff(Animation src, boolean downgradable, boolean forcePlayerAnim) throws IOException {
        return new EmotePacket.Builder()
                .configureToSaveEmote(src)
                .setVersion(downgradable ? getDowngradedHashMap(src, forcePlayerAnim) : new HashMap<>())
                .build(Integer.MAX_VALUE, false)
                .write();
    }

    @SuppressWarnings("unused")
    public static HashMap<Byte, Byte> getDowngradedHashMap(Animation animation, boolean forcePlayerAnim) {
        HashMap<Byte, Byte> version = new HashMap<>(EmotePacket.defaultVersions);

        if (forcePlayerAnim || animation.data().isAnimationPlayerAnimatorFormat()) {
            version.remove(PacketConfig.NEW_ANIMATION_FORMAT);

            if (KeyframeUtils.hasEasingArgs(animation)) {
                version.put((byte) 0, (byte) 4);
            } else if (KeyframeUtils.hasScaling(animation)) {
                version.put((byte) 0, (byte) 3);
            } else if (KeyframeUtils.hasDynamicParts(animation)) {
                version.put((byte) 0, (byte) 2);
            } else {
                version.put((byte) 0, (byte) 1);
            }
        } else {
            // TODO It may be possible to downgrade PAL versions, but for now this is not necessary.
            version.remove(PacketConfig.LEGACY_ANIMATION_FORMAT);
        }

        if (animation.data().has("bages")) {
            version.put((byte) 0x11, (byte) 2);
        } else {
            version.put((byte) 0x11, (byte) 1);
        }

        return version;
    }
}
