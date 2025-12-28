package org.redlance.common.utils.requester.boosty.obj.profile;

import org.redlance.common.utils.requester.boosty.obj.user.SubscriptionLevel;

public class Subscription {
    /**
     * User's last payment.
     */
    public long lastPayTime;

    /**
     * User's next payment (if subscription is active).
     */
    public long nextPayTime;

    /**
     * Date when the user bought the last subscription.
     */
    public long onTime;

    /**
     * Date when subscription will end, set only if user canceled auto-renewal.
     */
    public long offTime;

    /**
     * SubscriptionLevel status, can be: suspended, active, expired, renewal_canceled.
     */
    public String status;

    /**
     * User subscription level.
     */
    public SubscriptionLevel subscriptionLevel;

    @Override
    public String toString() {
        return String.format("Subscription{%s (%s)}", subscriptionLevel, status);
    }
}
