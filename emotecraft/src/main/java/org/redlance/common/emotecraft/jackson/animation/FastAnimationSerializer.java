package org.redlance.common.emotecraft.jackson.animation;

import com.zigythebird.playeranimcore.animation.Animation;
import io.github.kosmx.emotes.common.network.EmotePacket;
import io.github.kosmx.emotes.common.network.PacketConfig;
import io.github.kosmx.emotes.common.network.PacketTask;
import io.github.kosmx.emotes.common.tools.MathHelper;
import io.netty.buffer.AdaptiveByteBufAllocator;
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
    protected static final ByteBufAllocator ALLOC = AdaptiveByteBufAllocator.DEFAULT;

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
            packet.write(buf, ALLOC);
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
                version.put((byte) 0, (byte) 4);
            } else if (KeyframeUtils.hasScaling(animation)) {
                version.put((byte) 0, (byte) 3);
            } else if (KeyframeUtils.hasDynamicParts(animation)) {
                version.put((byte) 0, (byte) 2);
            } else {
                version.put((byte) 0, (byte) 1);
            }
        } else {
            // TODO It may be possible to downgrade PAL versions, but for now this is not necessary.
            version.remove(PacketConfig.LEGACY_ANIMATION_FORMAT);
        }

        if (animation.data().getRaw("bages") instanceof List<?> tags && !tags.isEmpty()) {
            version.put((byte) 0x11, (byte) 2);
        } else {
            version.put((byte) 0x11, (byte) 1);
        }

        return version;
    }
}
