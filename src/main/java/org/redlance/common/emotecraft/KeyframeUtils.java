package org.redlance.common.emotecraft;

import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class KeyframeUtils {
    private static final List<String> KEYS = List.of(
            "head", "body", "rightArm", "leftArm", "rightLeg", "leftLeg"
    );

    public static boolean hasEasingArgs(KeyframeAnimation animation) {
        for (KeyframeAnimation.StateCollection collection : animation.getBodyParts().values()) {
            if (collectionToStates(collection).anyMatch(KeyframeUtils::hasEasingArgs)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasEasingArgs(KeyframeAnimation.StateCollection.State state) {
        if (!isStateUsed(state)) return false;
        for (KeyframeAnimation.KeyFrame frame : state.getKeyFrames()) {
            if (frame.easingArg != null && !frame.easingArg.isNaN()) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasBends(KeyframeAnimation animation) {
        for (KeyframeAnimation.StateCollection collection : animation.getBodyParts().values()) {
            if (isStateUsed(collection.bend) || isStateUsed(collection.bendDirection)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasScaling(KeyframeAnimation animation) {
        for (KeyframeAnimation.StateCollection part : animation.getBodyParts().values()) {
            if (!part.isScalable()) continue;
            if (isStateUsed(part.scaleX) || isStateUsed(part.scaleY) || isStateUsed(part.scaleZ)) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasDynamicParts(KeyframeAnimation animation) {
        for (Map.Entry<String, KeyframeAnimation.StateCollection> entry : animation.getBodyParts().entrySet()) {
            if (KEYS.contains(entry.getKey())) continue;
            if (isCollectionUsed(entry.getValue())) {
                return true;
            }
        }

        return false;
    }

    public static boolean isCollectionUsed(@Nullable KeyframeAnimation.StateCollection collection) {
        if (collection == null) return false;
        return collectionToStates(collection).anyMatch(KeyframeUtils::isStateUsed);
    }

    public static boolean isStateUsed(@Nullable KeyframeAnimation.StateCollection.State state) {
        return state != null && !state.getKeyFrames().isEmpty() && state.isEnabled();
    }

    public static Stream<KeyframeAnimation.StateCollection.State> collectionToStates(KeyframeAnimation.StateCollection collection) {
        return Stream.of(
                collection.x, collection.y, collection.z,
                collection.yaw, collection.pitch, collection.roll,
                collection.bendDirection, collection.bend,
                collection.scaleX, collection.scaleY, collection.scaleZ
        );
    }
}
