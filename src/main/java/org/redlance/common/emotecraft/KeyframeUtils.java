package org.redlance.common.emotecraft;

import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class KeyframeUtils {
    private static final List<String> KEYS = List.of(
            "head", "body", "rightArm", "leftArm", "rightLeg", "leftLeg"
    );

    public static boolean hasBends(KeyframeAnimation animation) {
        for (KeyframeAnimation.StateCollection collection : animation.getBodyParts().values()) {
            if (hasBends(collection)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasBends(KeyframeAnimation.StateCollection collection) {
        if (!collection.isBendable()) {
            return false;
        }

        boolean hasBends = false;
        if (collection.bend != null) {
            hasBends |= isStateUsed(collection.bend);
        }
        if (collection.bendDirection != null) {
            hasBends |= isStateUsed(collection.bendDirection);
        }
        return hasBends;
    }

    public static boolean hasScaling(KeyframeAnimation animation) {
        for (KeyframeAnimation.StateCollection part : animation.getBodyParts().values()) {
            if (!part.isScalable()) {
                continue;
            }

            if (isStateUsed(part.scaleX) || isStateUsed(part.scaleY) || isStateUsed(part.scaleZ)) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasDynamicParts(KeyframeAnimation animation) {
        for (Map.Entry<String, KeyframeAnimation.StateCollection> entry : animation.getBodyParts().entrySet()) {
            if (KEYS.contains(entry.getKey())) {
                continue;
            }

            if (isCollectionUsed(entry.getValue())) {
                return true;
            }
        }

        return false;
    }

    public static boolean isCollectionUsed(KeyframeAnimation.StateCollection collection) {
        return isStateUsed(collection.x) || isStateUsed(collection.y) || isStateUsed(collection.z) ||
                isStateUsed(collection.yaw) || isStateUsed(collection.pitch) || isStateUsed(collection.roll) ||
                isStateUsed(collection.bendDirection) || isStateUsed(collection.bend) ||
                isStateUsed(collection.scaleX) || isStateUsed(collection.scaleY) || isStateUsed(collection.scaleZ);
    }

    public static boolean isStateUsed(@Nullable KeyframeAnimation.StateCollection.State state) {
        return state != null && !state.getKeyFrames().isEmpty() && state.isEnabled();
    }
}
