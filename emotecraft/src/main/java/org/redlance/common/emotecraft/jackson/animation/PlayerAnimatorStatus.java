package org.redlance.common.emotecraft.jackson.animation;

import com.zigythebird.playeranimcore.animation.Animation;

import java.util.function.Function;

@SuppressWarnings("unused") // API
public enum PlayerAnimatorStatus implements Function<Animation, Boolean> {
    TRUE(_ -> true),
    FALSE(_ -> false),

    IF_PLAYER_ANIMATOR(animation -> animation != null && animation.data().isAnimationPlayerAnimatorFormat());

    private final Function<Animation, Boolean> downgrade;

    PlayerAnimatorStatus(Function<Animation, Boolean> downgrade) {
        this.downgrade = downgrade;
    }

    @Override
    public Boolean apply(Animation animation) {
        return this.downgrade.apply(animation);
    }
}
