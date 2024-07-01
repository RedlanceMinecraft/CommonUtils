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

public class FastAnimationSerializer implements JsonDeserializer<KeyframeAnimation>, JsonSerializer<KeyframeAnimation> {
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
                    .build()
                    .write();

            String base64 = Base64.getEncoder().encodeToString(
                    AbstractNetworkInstance.safeGetBytesFromBuffer(byteBuffer)
            );

            return context.serialize(base64);
        } catch (Throwable e) {
            CommonUtils.LOGGER.error("Failed to serialize animation {}!", src, e);

            return JsonNull.INSTANCE;
        }
    }
}
