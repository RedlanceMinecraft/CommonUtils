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
import org.jetbrains.annotations.Nullable;
import org.redlance.common.CommonUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            version.put((byte) 0, (byte) 3);

        } else if (hasDynamicParts(animation)) {
            version.put((byte) 0, (byte) 2);

        } else {
            version.put((byte) 0, (byte) 1);
        }

        return version;
    }

    private static boolean hasScaling(KeyframeAnimation animation) {
        for (KeyframeAnimation.StateCollection part : animation.getBodyParts().values()) {
            if (!part.isScalable()) {
                continue;
            }

            if (isStateUsed(part.scaleX) || isStateUsed(part.scaleY) || isStateUsed(part.scaleZ)) {
                return true;
            }
        }

        return false;
    }

    private static final List<String> KEYS = List.of(
            "head", "body", "rightArm", "leftArm", "rightLeg", "leftLeg"
    );

    private static boolean hasDynamicParts(KeyframeAnimation animation) {
        for (Map.Entry<String, KeyframeAnimation.StateCollection> entry : animation.getBodyParts().entrySet()) {
            if (KEYS.contains(entry.getKey())) {
                continue;
            }

            if (isCollectionUsed(entry.getValue())) {
                return true;
            }
        }

        return false;
    }

    private static boolean isCollectionUsed(KeyframeAnimation.StateCollection collection) {
        return isStateUsed(collection.x) || isStateUsed(collection.y) || isStateUsed(collection.z) ||
                isStateUsed(collection.yaw) || isStateUsed(collection.pitch) || isStateUsed(collection.roll) ||
                isStateUsed(collection.bendDirection) || isStateUsed(collection.bend) ||
                isStateUsed(collection.scaleX) || isStateUsed(collection.scaleY) || isStateUsed(collection.scaleZ);
    }

    private static boolean isStateUsed(@Nullable KeyframeAnimation.StateCollection.State state) {
        return state != null && !state.getKeyFrames().isEmpty() && state.isEnabled();
    }
}
