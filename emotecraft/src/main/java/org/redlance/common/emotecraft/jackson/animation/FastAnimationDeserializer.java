package org.redlance.common.emotecraft.jackson.animation;

import com.zigythebird.playeranimcore.animation.Animation;
import io.github.kosmx.emotes.common.serializer.gson.AnimationTypeAdapter;
import org.redlance.common.emotecraft.jackson.EmotecraftModule;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;

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
            return AnimationTypeAdapter.fromBytes(bytes);
        } catch (Throwable e) {
            EmotecraftModule.LOGGER.warn("Failed to deserialize animation from binary JSON node {}", t, e);
            return null;
        }
    }
}
