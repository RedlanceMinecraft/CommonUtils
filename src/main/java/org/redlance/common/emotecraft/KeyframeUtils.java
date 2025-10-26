package org.redlance.common.emotecraft;

import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.keyframe.BoneAnimation;
import com.zigythebird.playeranimcore.animation.keyframe.Keyframe;
import com.zigythebird.playeranimcore.animation.keyframe.KeyframeStack;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class KeyframeUtils {
    private static final List<String> KEYS = List.of(
            "head", "body", "right_arm", "left_arm", "right_leg", "left_leg"
    );

    public static boolean hasEasingArgs(Animation animation) {
        for (BoneAnimation bone : animation.boneAnimations().values()) {
            if (hasEasingArgs(bone.positionKeyFrames()) || hasEasingArgs(bone.rotationKeyFrames())
                    || hasEasingArgs(bone.scaleKeyFrames()) || hasEasingArgs(bone.bendKeyFrames())) return true;
        }
        return false;
    }

    public static boolean hasEasingArgs(KeyframeStack stack) {
        return hasEasingArgs(stack.xKeyframes()) || hasEasingArgs(stack.yKeyframes()) || hasEasingArgs(stack.zKeyframes());
    }

    public static boolean hasEasingArgs(List<Keyframe> stack) {
        for (Keyframe keyframe : stack) {
            if (!keyframe.easingArgs().isEmpty()) return true;
        }
        return false;
    }

    public static boolean hasBends(Animation animation) {
        for (BoneAnimation bone : animation.boneAnimations().values()) {
            if (!bone.bendKeyFrames().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasScaling(Animation animation) {
        for (BoneAnimation bone : animation.boneAnimations().values()) {
            if (bone.scaleKeyFrames().hasKeyframes()) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasDynamicParts(Animation animation) {
        for (Map.Entry<String, BoneAnimation> entry : animation.boneAnimations().entrySet()) {
            if (KEYS.contains(entry.getKey())) continue;
            if (entry.getValue().hasKeyframes()) {
                return true;
            }
        }

        return false;
    }
}
