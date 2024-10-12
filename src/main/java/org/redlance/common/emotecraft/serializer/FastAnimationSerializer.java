package org.redlance.common.emotecraft.serializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import dev.kosmx.playerAnim.core.data.AnimationFormat;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import io.github.kosmx.emotes.api.proxy.AbstractNetworkInstance;
import io.github.kosmx.emotes.common.network.EmotePacket;
import io.github.kosmx.emotes.server.serializer.UniversalEmoteSerializer;
import org.redlance.common.CommonUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.HashMap;

public class FastAnimationSerializer implements JsonDeserializer<KeyframeAnimation>, JsonSerializer<KeyframeAnimation> {
    @SuppressWarnings("deprecation")
    public static final KeyframeAnimation INVALID = new KeyframeAnimation.AnimationBuilder(AnimationFormat.UNKNOWN)
            .setName("{\"color\":\"red\",\"text\":\"INVALID\"}")
            .build();

    public static final FastAnimationSerializer INSTANCE = new FastAnimationSerializer();

    private FastAnimationSerializer() {
        CommonUtils.LOGGER.debug("Serializing animations via {}!", getClass().getName());
    }

    @Override
    public KeyframeAnimation deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        try (InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(json.getAsString()))) {
            return UniversalEmoteSerializer.readData(is, "emote.emotecraft").getFirst();

        } catch (Throwable e) {
            CommonUtils.LOGGER.warn("Failed to deserialize animation {}!", json, e);

            return FastAnimationSerializer.INVALID;
        }
    }

    @Override
    public JsonElement serialize(KeyframeAnimation src, Type type, JsonSerializationContext context) {
        try {
            ByteBuffer byteBuffer = new EmotePacket.Builder()
                    .configureToSaveEmote(src)
                    .setVersion(getDowngradedHashMap(src))
                    .build(Integer.MAX_VALUE)
                    .write();

            return context.serialize(Base64.getEncoder().encodeToString(
                    AbstractNetworkInstance.safeGetBytesFromBuffer(byteBuffer)
            ));
        } catch (Throwable e) {
            CommonUtils.LOGGER.error("Failed to serialize animation {}!", src, e);

            return JsonNull.INSTANCE;
        }
    }

    public static HashMap<Byte, Byte> getDowngradedHashMap(KeyframeAnimation animation) {
        HashMap<Byte, Byte> version = new HashMap<>();

        if (hasScaling(animation)) {
            CommonUtils.LOGGER.warn("{} requires version 3, which is unsupported in most cases!", animation);
        } else {
            version.put((byte) 0, (byte) 2); // Defailt is 3 for scaling
        }

        return version;
    }

    private static boolean hasScaling(KeyframeAnimation animation) {
        for (KeyframeAnimation.StateCollection part : animation.getBodyParts().values()) {
            if (!part.isScalable()) {
                continue;
            }

            if (hasScaling(part.scaleX)) {
                return true;
            }

            if (hasScaling(part.scaleY)) {
                return true;
            }

            if (hasScaling(part.scaleZ)) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasScaling(KeyframeAnimation.StateCollection.State state) {
        return state != null && !state.getKeyFrames().isEmpty() && state.isEnabled();
    }
}
