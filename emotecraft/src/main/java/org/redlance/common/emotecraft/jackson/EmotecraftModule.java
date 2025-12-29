package org.redlance.common.emotecraft.jackson;

import com.zigythebird.playeranimcore.animation.Animation;
import org.redlance.common.emotecraft.jackson.animation.FastAnimationDeserializer;
import org.redlance.common.emotecraft.jackson.animation.FastAnimationSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.module.SimpleModule;

public class EmotecraftModule extends SimpleModule {
    public static final Logger LOGGER = LoggerFactory.getLogger("EmotecraftModule");

    public EmotecraftModule() {
        super("Emotecraft Jackson");
        addSerializer(Animation.class, FastAnimationSerializer.INSTANCE);
        addDeserializer(Animation.class, FastAnimationDeserializer.INSTANCE);
    }
}
