package org.redlance.common.emotecraft.jackson.animation;

import com.zigythebird.playeranimcore.animation.Animation;
import io.github.kosmx.emotes.common.network.EmotePacket;
import io.github.kosmx.emotes.common.network.PacketConfig;
import io.github.kosmx.emotes.common.network.PacketTask;
import io.github.kosmx.emotes.common.tools.MathHelper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.redlance.common.emotecraft.KeyframeUtils;
import org.redlance.common.emotecraft.jackson.EmotecraftModule;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;

public class FastAnimationSerializer extends ValueSerializer<Animation> {
    protected static final ByteBufAllocator ALLOC = ByteBufAllocator.DEFAULT;

    private final PacketTask packetTask;
    private final boolean downgradable;
    private final boolean forcePlayerAnim;

    public FastAnimationSerializer(PacketTask packetTask, boolean downgradable, boolean forcePlayerAnim) {
        if (packetTask != PacketTask.FILE && packetTask != PacketTask.STREAM) throw new UnsupportedOperationException();
        this.packetTask = packetTask;

        this.downgradable = downgradable;
        this.forcePlayerAnim = forcePlayerAnim;
    }

    @Override
    public void serialize(Animation src, JsonGenerator gen, SerializationContext ctx) throws JacksonException {
        if (src == null) {
            gen.writeNull();
            return;
        }

        try {
            gen.writeBinary(serializeToBytes(src, this.packetTask, this.downgradable, this.forcePlayerAnim));
        } catch (Throwable e) {
            EmotecraftModule.LOGGER.error("Failed to serialize animation {}!", src, e);
            gen.writeNull();
        }
    }

    public static String serializeToString(Animation src, PacketTask task, boolean downgradable, boolean forcePlayerAnim) {
        return Base64.getEncoder().encodeToString(serializeToBytes(src, task, downgradable, forcePlayerAnim));
    }

    public static byte[] serializeToBytes(Animation src, PacketTask task, boolean downgradable, boolean forcePlayerAnim) {
        if (task != PacketTask.FILE && task != PacketTask.STREAM) throw new UnsupportedOperationException();

        ByteBuf buf = ALLOC.buffer();

        try {
            EmotePacket packet = new EmotePacket.Builder()
                    .configureToSaveEmote(src)
                    .setVersion(downgradable ? getDowngradedHashMap(src, forcePlayerAnim) : new HashMap<>())
                    .build(Integer.MAX_VALUE, false);

            packet.data.purpose = task;
            packet.write(buf);
            return MathHelper.readBytes(buf);
        } finally {
            buf.release();
        }
    }

    @SuppressWarnings("unused")
    public static HashMap<Byte, Byte> getDowngradedHashMap(Animation animation, boolean forcePlayerAnim) {
        HashMap<Byte, Byte> version = new HashMap<>(EmotePacket.defaultVersions);

        if (forcePlayerAnim || animation.data().isAnimationPlayerAnimatorFormat()) {
            version.remove(PacketConfig.NEW_ANIMATION_FORMAT);

            if (KeyframeUtils.hasEasingArgs(animation)) {
                version.put(PacketConfig.LEGACY_ANIMATION_FORMAT, (byte) 4);
            } else if (KeyframeUtils.hasScaling(animation)) {
                version.put(PacketConfig.LEGACY_ANIMATION_FORMAT, (byte) 3);
            } else if (KeyframeUtils.hasDynamicParts(animation)) {
                version.put(PacketConfig.LEGACY_ANIMATION_FORMAT, (byte) 2);
            } else {
                version.put(PacketConfig.LEGACY_ANIMATION_FORMAT, (byte) 1);
            }
        } else {
            version.remove(PacketConfig.LEGACY_ANIMATION_FORMAT);
            version.put(PacketConfig.NEW_ANIMATION_FORMAT, (byte) 5);
        }

        if (animation.data().getRaw("bages") instanceof List<?> tags && !tags.isEmpty()) {
            version.put(PacketConfig.HEADER_PACKET, (byte) 2);
        } else {
            version.put(PacketConfig.HEADER_PACKET, (byte) 1);
        }

        return version;
    }
}
