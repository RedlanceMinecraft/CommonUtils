package org.redlance.common.emotecraft.jackson.animation;

import com.zigythebird.playeranimcore.animation.Animation;
import io.github.kosmx.emotes.common.network.EmotePacket;
import io.github.kosmx.emotes.common.network.PacketTask;
import io.github.kosmx.emotes.common.network.objects.NetData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.redlance.common.emotecraft.jackson.EmotecraftModule;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;

import java.util.Base64;

public class FastAnimationDeserializer extends ValueDeserializer<Animation> {
    public static final FastAnimationDeserializer INSTANCE = new FastAnimationDeserializer();

    private FastAnimationDeserializer() {
        super();
    }

    @Override
    public Animation deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonToken t = p.currentToken();
        if (t == null) t = p.nextToken();

        byte[] bytes;
        if (t == JsonToken.VALUE_EMBEDDED_OBJECT) {
            Object embedded = p.getEmbeddedObject();
            if (!(embedded instanceof byte[] b)) {
                throw MismatchedInputException.from(
                        p, "Expected embedded byte[] for Animation, got: " + (embedded == null ? "null" : embedded.getClass()));
            }
            bytes = b;
        } else {
            bytes = p.getBinaryValue();
        }

        try {
            return serialize(bytes);
        } catch (Throwable e) {
            EmotecraftModule.LOGGER.warn("Failed to deserialize animation from binary JSON node {}", t, e);
            return null;
        }
    }

    public Animation serialize(String src) {
        return serialize(Base64.getDecoder().decode(src));
    }

    public Animation serialize(byte[] src) {
        ByteBuf wrappedBuffer = Unpooled.wrappedBuffer(src);

        try {
            NetData data = new EmotePacket(wrappedBuffer).data;
            if (data.purpose != PacketTask.FILE) throw new IllegalStateException("Binary emote is invalid!");
            return data.emoteData;
        } finally {
            wrappedBuffer.release();
        }
    }
}
