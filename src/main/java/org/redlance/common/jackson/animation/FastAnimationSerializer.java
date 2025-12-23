package org.redlance.common.jackson.animation;

import com.zigythebird.playeranimcore.animation.Animation;
import io.github.kosmx.emotes.common.network.EmotePacket;
import io.github.kosmx.emotes.common.network.PacketConfig;
import io.netty.buffer.AdaptiveByteBufAllocator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.redlance.common.CommonUtils;
import org.redlance.common.emotecraft.KeyframeUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;

public class FastAnimationSerializer extends ValueSerializer<Animation> {
    protected static final ByteBufAllocator ALLOC = AdaptiveByteBufAllocator.DEFAULT;

    public static final FastAnimationSerializer INSTANCE = new FastAnimationSerializer(false, false);
    public static final FastAnimationSerializer DOWNGRADABLE = new FastAnimationSerializer(true, false);
    public static final FastAnimationSerializer LEGACY = new FastAnimationSerializer(true, true);

    private final boolean downgradable;
    private final boolean forcePlayerAnim;

    private FastAnimationSerializer(boolean downgradable, boolean forcePlayerAnim) {
        this.downgradable = downgradable;
        this.forcePlayerAnim = forcePlayerAnim;
        CommonUtils.LOGGER.debug("Serializing animations via {} (downgradable={}, forcePlayerAnim={})!",
                getClass().getName(), downgradable, forcePlayerAnim
        );
    }

    @Override
    public void serialize(Animation src, JsonGenerator gen, SerializationContext ctx) throws JacksonException {
        if (src == null) {
            gen.writeNull();
            return;
        }

        try {
            gen.writeBinary(serializeToBytes(src, this.downgradable, this.forcePlayerAnim));
        } catch (Throwable e) {
            CommonUtils.LOGGER.error("Failed to serialize animation {}!", src, e);
            gen.writeNull();
        }
    }

    public static String serializeToString(Animation src, boolean downgradable, boolean forcePlayerAnim) {
        return Base64.getEncoder().encodeToString(serializeToBytes(src, downgradable, forcePlayerAnim));
    }

    public static byte[] serializeToBytes(Animation src, boolean downgradable, boolean forcePlayerAnim) {
        ByteBuf buf = ALLOC.buffer();

        try {
            new EmotePacket.Builder()
                    .configureToSaveEmote(src)
                    .setVersion(downgradable ? getDowngradedHashMap(src, forcePlayerAnim) : new HashMap<>())
                    .build(Integer.MAX_VALUE, false)
                    .write(buf, ALLOC);

            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return bytes;
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
