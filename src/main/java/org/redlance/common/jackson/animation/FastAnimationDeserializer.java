package org.redlance.common.jackson.animation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.zigythebird.playeranimcore.animation.Animation;
import io.github.kosmx.emotes.common.network.EmotePacket;
import io.github.kosmx.emotes.common.network.PacketTask;
import io.github.kosmx.emotes.common.network.objects.NetData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.redlance.common.CommonUtils;

import java.io.IOException;
import java.util.Base64;

public class FastAnimationDeserializer extends JsonDeserializer<Animation> {
    public static final FastAnimationDeserializer INSTANCE = new FastAnimationDeserializer();

    private FastAnimationDeserializer() {
        super();
    }

    @Override
    public Animation deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        byte[] bytes;
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isBinary() || node.isTextual()) {
            bytes = node.binaryValue();
        } /*else if (node.isTextual()) {
            try {
                bytes = Base64.getDecoder().decode(node.asText());
            } catch (IllegalArgumentException e) {
                throw JsonMappingException.from(p, "Expected binary or base64 string for Animation", e);
            }
        }*/ else {
            throw JsonMappingException.from(
                    p, "Expected binary or base64 string for Animation, got: " + node.getNodeType()
            );
        }

        try {
            return serialize(bytes);
        } catch (Throwable e) {
            CommonUtils.LOGGER.warn("Failed to deserialize animation from binary JSON node {}", node, e);
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
