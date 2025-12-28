package org.redlance.common.utils.requester.boosty.obj.user;

import org.redlance.common.utils.requester.Chunker;
import org.redlance.common.utils.requester.boosty.BoostyRequester;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class BoostyUser {
    /**
     * If you can write to this user.
     * Accessible only through {@link BoostyRequester#requestSubscribersChunking(Chunker, String, String, long)},
     * {@link BoostyRequester#requestPostSalesChunking(Chunker, String, String)} (String, String)}
     */
    public boolean canWrite;

    /**
     * Link to user avatar.
     * If there is no avatar, the value will be empty, not null.
     */
    public String avatarUrl;

    /**
     * Whether the user has an avatar set.
     */
    public boolean hasAvatar;

    /**
     * User e-mail.
     */
    public String email;

    /**
     * Unique user id.
     */
    public long id;

    /**
     * true if a user has a blog page.
     * Accessible only through {@link BoostyRequester#requestUserProfile(String, String, long)}
     */
    public boolean isBlogger;

    /**
     * Link to user's blog (if available)
     * Accessible only through {@link BoostyRequester#requestUserProfile(String, String, long)}
     */
    public String blogUrl;

    /**
     * User name.
     */
    public String name;

    /**
     * true if the user is on **your** blacklist.
     * Accessible only through {@link BoostyRequester#requestSubscribersChunking(Chunker, String, String, long)}
     */
    public boolean isBlackListed;

    /**
     * Was the fee paid off when purchasing subscription?
     */
    public boolean isFeePaid;

    /**
     * User subscription level.
     * Accessible only through {@link BoostyRequester#requestSubscribersChunking(Chunker, String, String, long)}
     */
    public SubscriptionLevel level;

    /**
     * The date when the next payment is due if the user has an active subscription.
     * Accessible only through {@link BoostyRequester#requestSubscribersChunking(Chunker, String, String, long)}
     */
    public long nextPayTime;

    /**
     * Date when the user bought the last subscription.
     * Accessible only through {@link BoostyRequester#requestSubscribersChunking(Chunker, String, String, long)}
     */
    public long onTime;

    /**
     * Date when subscription will end, set only if user canceled auto-renewal.
     * Accessible only through {@link BoostyRequester#requestSubscribersChunking(Chunker, String, String, long)}
     */
    public long offTime;

    /**
     * Amount paid by the user for the entire time.
     * Accessible only through {@link BoostyRequester#requestSubscribersChunking(Chunker, String, String, long)}
     */
    public double payments;

    /**
     * The amount for which the user purchased the subscription.
     * Accessible only through {@link BoostyRequester#requestSubscribersChunking(Chunker, String, String, long)}
     */
    public int price;

    /**
     * Subscription Discount (Or Gift Link).
     */
    public BoostyPromo promo;

    /**
     * true if the user is subscribed right now, also see {@link #isSubscribed()}
     * Accessible only through {@link BoostyRequester#requestSubscribersChunking(Chunker, String, String, long)}
     */
    public boolean subscribed;

    /**
     * Linked social networks, currently telegram and discord are available.
     * Accessible only through {@link BoostyRequester#requestUserProfile(String, String, long)}
     */
    public Map<String, ExternalApp> externalApps = new HashMap<>();

    /**
     * User subscription status
     * Accessible only through {@link BoostyRequester#requestSubscribersChunking(Chunker, String, String, long)}
     */
    public SubscribeStatus status;

    /**
     * Checks the user's subscription, and also checks the time.
     */
    public boolean isSubscribed() {
        if (onTime > 0) {
            long seconds = Instant.now(Clock.systemUTC()).getEpochSecond() - 86400;

            if (nextPayTime > 0) { // subscribed
                return nextPayTime > seconds;
            }

            if (offTime > 0) { // unsubscribed
                return offTime > seconds;
            }

            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return String.format("BoostyUser{%s - %s (%s)}", name, email, id);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof BoostyUser boostyUser) {
            if (this.id == 0) {
                return false; // Invalid
            }

            return this.id == boostyUser.id;
        }

        if (other instanceof String string) {
            return Objects.equals(email, string) || Objects.equals(name, string);
        }

        if (other instanceof Number in) {
            return this.id == in.intValue();
        }

        return false;
    }
}
