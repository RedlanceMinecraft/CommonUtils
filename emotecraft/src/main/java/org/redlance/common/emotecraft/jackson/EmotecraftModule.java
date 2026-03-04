package org.redlance.common.emotecraft.jackson;

import com.zigythebird.playeranimcore.animation.Animation;
import io.github.kosmx.emotes.common.CommonData;
import io.github.kosmx.emotes.common.network.PacketTask;
import org.redlance.common.emotecraft.jackson.animation.FastAnimationDeserializer;
import org.redlance.common.emotecraft.jackson.animation.FastAnimationSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.module.SimpleModule;

public class EmotecraftModule extends SimpleModule {
    public static final Logger LOGGER = LoggerFactory.getLogger("EmotecraftModule");

    @SuppressWarnings("unused") // Called by ServiceLoader
    public EmotecraftModule() {
        this(false);
    }

    public EmotecraftModule(boolean compact) {
        super("Emotecraft Jackson");
        addSerializer(Animation.class, new FastAnimationSerializer(compact ? PacketTask.STREAM : PacketTask.FILE, false, false));
        addDeserializer(Animation.class, FastAnimationDeserializer.INSTANCE);
    }

    @Override
    public Object getRegistrationId() {
        return CommonData.MOD_ID;
    }
}
