package org.redlance.common.emotecraft.jackson;

import com.zigythebird.playeranimcore.animation.Animation;
import io.github.kosmx.emotes.common.CommonData;
import io.github.kosmx.emotes.common.network.PacketTask;
import org.redlance.common.emotecraft.jackson.animation.FastAnimationDeserializer;
import org.redlance.common.emotecraft.jackson.animation.FastAnimationSerializer;
import org.redlance.common.emotecraft.jackson.animation.PlayerAnimatorStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.module.SimpleModule;

public class EmotecraftModule extends SimpleModule {
    public static final Logger LOGGER = LoggerFactory.getLogger("EmotecraftModule");
    private static final EmotecraftModule COMPACT_MODULE = new EmotecraftModule(PacketTask.STREAM, true, PlayerAnimatorStatus.FALSE);


    @SuppressWarnings("unused") // Called by ServiceLoader
    public EmotecraftModule() {
        this(PacketTask.FILE, true, PlayerAnimatorStatus.FALSE);
    }

    public EmotecraftModule(PacketTask packetTask, boolean downgradable, PlayerAnimatorStatus playerAnimatorStatus) {
        super("Emotecraft Jackson");
        addSerializer(Animation.class, new FastAnimationSerializer(packetTask, downgradable, playerAnimatorStatus));
        addDeserializer(Animation.class, FastAnimationDeserializer.INSTANCE);
    }

    @Override
    public Object getRegistrationId() {
        return CommonData.MOD_ID;
    }

    public static ObjectMapper rebuildWithCompact(ObjectMapper mapper) {
        return mapper.rebuild().addModule(COMPACT_MODULE).build();
    }
}
