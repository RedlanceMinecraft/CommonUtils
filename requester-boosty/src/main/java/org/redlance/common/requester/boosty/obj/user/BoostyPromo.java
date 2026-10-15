package org.redlance.common.requester.boosty.obj.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redlance.common.requester.boosty.obj.BoostyValues;

import java.util.Map;

/**
 * Promotion created by the blog author: a trial link or a discount.
 *
 * @param id          Unique promotion id.
 * @param type        Kind of the promotion.
 * @param targetType  What the promotion applies to.
 * @param linkId      Code of the promo link; sent for trial links.
 * @param description Description set by the author; unset in observed responses.
 * @param startTime   Start of the promotion, Unix seconds.
 * @param endTime     End of the promotion, Unix seconds; {@code 0} when it has none.
 * @param isFinished  Whether the promotion is over.
 * @param count       Activation counters.
 * @param access      Who may use the promotion.
 * @param trial       Trial length; sent for trial links.
 * @param discount    Discounted price; sent for discounts.
 */
public record BoostyPromo(
        long id,
        Type type,
        TargetType targetType,
        @Nullable String linkId,
        @Nullable String description,
        long startTime,
        long endTime,
        boolean isFinished,
        @Nullable Count count,
        @Nullable Access access,
        @Nullable Trial trial,
        @Nullable Discount discount
) {
    public BoostyPromo {
        linkId = BoostyValues.nullIfEmpty(linkId);
        description = BoostyValues.nullIfEmpty(description);
    }

    @Override
    public @NotNull String toString() {
        return String.format("BoostyPromo{%s (%s)}", this.id, this.type);
    }

    /**
     * Kind of a promotion.
     */
    public enum Type {
        /**
         * Reduced price, see {@link BoostyPromo#discount()}.
         */
        DISCOUNT,

        /**
         * Free trial, see {@link BoostyPromo#trial()}.
         */
        TRIAL,

        /**
         * Free trial given by a private link, see {@link BoostyPromo#trial()} and {@link BoostyPromo#linkId()}.
         */
        TRIAL_LINK,

        /**
         * A kind this wrapper does not know yet.
         */
        UNKNOWN;

        /**
         * Reads a kind in any case; one this wrapper does not know becomes {@link #UNKNOWN}.
         */
        @JsonCreator
        public static Type of(String value) {
            return BoostyValues.parse(values(), value, UNKNOWN);
        }
    }

    /**
     * What a promotion applies to.
     */
    public enum TargetType {
        /**
         * Subscription to a level.
         */
        SUBSCRIPTION_LEVEL,

        /**
         * Paid post.
         */
        POST,

        /**
         * Bundle of posts.
         */
        BUNDLE,

        /**
         * A target this wrapper does not know yet.
         */
        UNKNOWN;

        /**
         * Reads a target in any case; one this wrapper does not know becomes {@link #UNKNOWN}.
         */
        @JsonCreator
        public static TargetType of(String value) {
            return BoostyValues.parse(values(), value, UNKNOWN);
        }
    }

    /**
     * Activation counters of a promotion.
     *
     * @param activation    How many times the promotion was used.
     * @param maxActivation Limit of uses; {@code 0} when unlimited.
     */
    public record Count(int activation, int maxActivation) {
    }

    /**
     * Audience of a promotion.
     *
     * @param newSubscriber              Users who never subscribed.
     * @param oldPaidSubscriber          Former paid subscribers.
     * @param follower                   Followers on the free level.
     * @param accessOtherLevelSubscriber Paid subscribers of another level.
     */
    public record Access(boolean newSubscriber, boolean oldPaidSubscriber, boolean follower,
                         boolean accessOtherLevelSubscriber) {
    }

    /**
     * Trial of a promotion.
     *
     * @param days Trial length in days; {@code 32767} (about 90 years) makes the trial practically permanent.
     */
    public record Trial(int days) {
    }

    /**
     * Discount of a promotion.
     *
     * @param percent        Discount in percent.
     * @param price          Discounted price in rubles.
     * @param period         Number of months the discounted price applies to.
     * @param currencyPrices Discounted price by currency, like {@link SubscriptionLevel#currencyPrices()}.
     */
    public record Discount(int percent, int price, int period, Map<Currency, Double> currencyPrices) {
        public Discount {
            currencyPrices = Currency.knownPrices(currencyPrices);
        }
    }
}
