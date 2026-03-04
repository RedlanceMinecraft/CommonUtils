package org.redlance.common.tests;

import com.zigythebird.playeranimcore.animation.Animation;
import io.github.kosmx.emotes.common.SerializableConfig;
import io.github.kosmx.emotes.common.network.PacketTask;
import io.github.kosmx.emotes.server.serializer.UniversalEmoteSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.redlance.common.emotecraft.EmoteCraftInstance;
import org.redlance.common.emotecraft.jackson.animation.FastAnimationDeserializer;
import org.redlance.common.emotecraft.jackson.animation.FastAnimationSerializer;
import org.redlance.common.jackson.JacksonMappers;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class FastAnimationSerializerTest {
    private static final Map<String, Animation> ANIMATION_MAP = new HashMap<>();

    @BeforeAll
    public static void initEmotecraft() throws IOException {
        EmoteCraftInstance.tryInitializeInstance(SerializableConfig::new, SerializableConfig.class);

        try (InputStream is = FastAnimationSerializerTest.class.getResourceAsStream("/test_test_cape_axes.json")) {
            ANIMATION_MAP.putAll(UniversalEmoteSerializer.readData(is, "emote.json"));
        }
    }

    @Test
    public void test() {
        String str = JacksonMappers.OBJECT_MAPPER.writeValueAsString(ANIMATION_MAP);
        Map<String, Animation> deserializedMap = JacksonMappers.OBJECT_MAPPER.readValue(str,
                JacksonMappers.OBJECT_MAPPER.getTypeFactory().constructMapType(Map.class, String.class, Animation.class)
        );

        Assertions.assertEquals(
                ANIMATION_MAP.values().iterator().next().boneAnimations(),
                deserializedMap.values().iterator().next().boneAnimations()
        );
    }

    @Test
    public void testStream() {
        Animation animation = ANIMATION_MAP.values().iterator().next();

        byte[] bytes = FastAnimationSerializer.serializeToBytes(animation, PacketTask.STREAM, false, false);
        Animation deserialized = FastAnimationDeserializer.INSTANCE.serialize(bytes);

        Assertions.assertEquals(animation.boneAnimations(), deserialized.boneAnimations());
    }
}
