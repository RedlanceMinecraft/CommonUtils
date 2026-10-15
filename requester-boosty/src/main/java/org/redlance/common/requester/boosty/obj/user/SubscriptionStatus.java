package org.redlance.common.requester.boosty.obj.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.redlance.common.requester.boosty.obj.BoostyValues;

/**
 * Subscription state, see {@link BoostyUser#status()}.
 * <p>
 * The subscriber list reports a coarse state: {@link #ACTIVE}, {@link #INACTIVE} or {@link #PAUSED}. The subscriber
 * profile reports a finer one: {@link #ACTIVE}, {@link #TRIAL}, {@link #RENEWAL_CANCELED}, {@link #PAUSED} or
 * {@link #EXPIRED}; the list reports its {@link #TRIAL} and {@link #RENEWAL_CANCELED} as {@link #ACTIVE}.
 */
public enum SubscriptionStatus {
    /**
     * The subscription runs. From the profile: it renews, with the next charge at {@link BoostyUser#nextPayTime()},
     * or it is on the free level. From the list: any running subscription.
     */
    ACTIVE(true),

    /**
     * Profile only: trial from a promo link, see {@link BoostyUser#promo()}; it lasts until
     * {@link BoostyUser#nextPayTime()}.
     */
    TRIAL(true),

    /**
     * Profile only: auto-renewal is turned off; access lasts until {@link BoostyUser#offTime()}.
     */
    RENEWAL_CANCELED(true),

    /**
     * The subscriber paused the subscription.
     */
    PAUSED(false),

    /**
     * List only: the subscription has ended.
     */
    INACTIVE(false),

    /**
     * Profile only: the subscription ended at {@link BoostyUser#offTime()}.
     */
    EXPIRED(false),

    /**
     * Not seen in current responses; kept from earlier versions of this wrapper.
     */
    SUSPENDED(false),

    /**
     * A state this wrapper does not know yet.
     */
    UNKNOWN(false);

    private final boolean active;

    SubscriptionStatus(boolean active) {
        this.active = active;
    }

    /**
     * Whether the subscription gives access in this state.
     */
    public boolean isActive() {
        return this.active;
    }

    /**
     * Reads a state in any case; one this wrapper does not know becomes {@link #UNKNOWN}.
     */
    @JsonCreator
    public static SubscriptionStatus of(String value) {
        return BoostyValues.parse(values(), value, UNKNOWN);
    }
}
