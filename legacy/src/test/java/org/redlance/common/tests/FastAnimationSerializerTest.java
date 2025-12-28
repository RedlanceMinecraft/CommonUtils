package org.redlance.common.tests;

import com.zigythebird.playeranimcore.animation.Animation;
import io.github.kosmx.emotes.common.SerializableConfig;
import io.github.kosmx.emotes.server.serializer.UniversalEmoteSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.redlance.common.CommonUtils;
import org.redlance.common.emotecraft.EmoteCraftInstance;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class FastAnimationSerializerTest {

    @BeforeAll
    public static void initEmotecraft() {
        EmoteCraftInstance.tryInitializeInstance(SerializableConfig::new, SerializableConfig.class);
    }

    @Test
    public void test() throws IOException {
        Map<String, Animation> animationMap = new HashMap<>();

        try (InputStream is = FastAnimationSerializerTest.class.getResourceAsStream("/test_test_cape_axes.json")) {
            animationMap.putAll(UniversalEmoteSerializer.readData(is, "emote.json"));
        }

        String str = CommonUtils.OBJECT_MAPPER.writeValueAsString(animationMap);
        Map<String, Animation> deserializedMap = CommonUtils.OBJECT_MAPPER.readValue(str,
                CommonUtils.OBJECT_MAPPER.getTypeFactory().constructMapType(Map.class, String.class, Animation.class)
        );

        Assertions.assertEquals(
                animationMap.values().iterator().next().boneAnimations(),
                deserializedMap.values().iterator().next().boneAnimations()
        );
    }
}
