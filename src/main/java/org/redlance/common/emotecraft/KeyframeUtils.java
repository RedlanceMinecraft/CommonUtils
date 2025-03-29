package org.redlance.common.emotecraft;

import dev.kosmx.playerAnim.core.data.KeyframeAnimation;

public class KeyframeUtils {
    private static boolean hasBends(KeyframeAnimation animation) {
        for (KeyframeAnimation.StateCollection collection : animation.getBodyParts().values()) {
            if (hasBends(collection)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasBends(KeyframeAnimation.StateCollection collection) {
        if (!collection.isBendable()) {
            return false;
        }

        boolean hasBends = false;
        if (collection.bend != null) {
            hasBends |= hasBends(collection.bend);
        }
        if (collection.bendDirection != null) {
            hasBends |= hasBends(collection.bendDirection);
        }
        return hasBends;
    }

    private static boolean hasBends(KeyframeAnimation.StateCollection.State state) {
        return state.isEnabled() && !state.getKeyFrames().isEmpty();
    }
}
