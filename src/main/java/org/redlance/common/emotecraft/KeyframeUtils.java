package org.redlance.common.emotecraft;

import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.keyframe.BoneAnimation;
import com.zigythebird.playeranimcore.animation.keyframe.Keyframe;
import com.zigythebird.playeranimcore.enums.AnimationFormat;
import org.jetbrains.annotations.Nullable;
import team.unnamed.mocha.parser.ast.Expression;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class KeyframeUtils {
    private static final List<String> KEYS = List.of(
            "head", "body", "right_arm", "left_arm", "right_leg", "left_leg"
    );

    public static boolean hasEasingArgs(Animation animation) {
        for (BoneAnimation collection : animation.boneAnimations().values()) {
            if (collectionToStates(collection).anyMatch(KeyframeUtils::hasEasingArgs)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasEasingArgs(List<Keyframe> state) {
        if (!isStateUsed(state)) return false;
        for (Keyframe frame : state) {
            List<List<Expression>> easingArgs = frame.easingArgs();
            if (easingArgs == null || easingArgs.isEmpty()) {
                return false;
            }
            if (easingArgs.size() == 1) {
                return !easingArgs.getFirst().isEmpty();
            }
            return true;
        }
        return false;
    }

    public static boolean hasBends(Animation animation) {
        for (BoneAnimation collection : animation.boneAnimations().values()) {
            if (isStateUsed(collection.bendKeyFrames())) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasScaling(Animation animation) {
        for (BoneAnimation part : animation.boneAnimations().values()) {
            if (part.scaleKeyFrames().hasKeyframes()) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasDynamicParts(Animation animation) {
        for (Map.Entry<String, BoneAnimation> entry : animation.boneAnimations().entrySet()) {
            if (KEYS.contains(entry.getKey())) continue;
            if (isCollectionUsed(entry.getValue())) {
                return true;
            }
        }

        return false;
    }

    public static boolean isCollectionUsed(@Nullable BoneAnimation collection) {
        if (collection == null) return false;
        return collectionToStates(collection).anyMatch(KeyframeUtils::isStateUsed);
    }

    public static boolean isStateUsed(@Nullable List<Keyframe> state) {
        return state != null && !state.isEmpty();
    }

    public static Stream<List<Keyframe>> collectionToStates(BoneAnimation collection) {
        return Stream.of(
                collection.positionKeyFrames().xKeyframes(), collection.positionKeyFrames().yKeyframes(), collection.positionKeyFrames().zKeyframes(),
                collection.rotationKeyFrames().xKeyframes(), collection.rotationKeyFrames().yKeyframes(), collection.rotationKeyFrames().zKeyframes(),
                collection.bendKeyFrames(),
                collection.scaleKeyFrames().xKeyframes(), collection.scaleKeyFrames().yKeyframes(), collection.scaleKeyFrames().zKeyframes()
        );
    }

    public static boolean isPlayerAnimatorFormat(Animation animation) {
        return animation.data().<AnimationFormat>get("format").orElse(null) == AnimationFormat.PLAYER_ANIMATOR;
    }
}
