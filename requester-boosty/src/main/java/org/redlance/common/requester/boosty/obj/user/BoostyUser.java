package org.redlance.common.requester.boosty.obj.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redlance.common.requester.Chunker;
import org.redlance.common.requester.boosty.BoostyRequester;
import org.redlance.common.requester.boosty.obj.BoostyValues;
import org.redlance.common.requester.boosty.obj.LenientDeserializer;
import org.redlance.common.requester.boosty.obj.post.PostSale;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

/**
 * Boosty user as the owner of a blog sees them.
 * <p>
 * Boosty describes users in several places, and each fills its own part of this record:
 * <ul>
 *     <li>subscriber list ({@link BoostyRequester#requestSubscribersChunking(Chunker, String, String, long...)}):
 *     the user and their subscription;</li>
 *     <li>subscriber profile ({@link BoostyRequester#requestUserProfile(String, String, long)}): the user with their
 *     blog and linked accounts, the subscription with its last payment, refunds, the note and both blacklists,
 *     parsed by {@link BoostyProfileDeserializer};</li>
 *     <li>post sale ({@link PostSale#user()}): the user alone.</li>
 * </ul>
 * Components their source does not send are {@code 0}, {@code false}, {@code null} or empty; an empty text is
 * {@code null} too. Times are Unix seconds, money is in rubles. Two instances are equal when all their components
 * are; compare {@link #id()} to tell whether they describe the same user.
 *
 * @param id                  Unique user id.
 * @param name                Display name.
 * @param email               E-mail; {@code null} when the account has none.
 * @param avatarUrl           Avatar URL; {@code null} without an avatar.
 * @param hasAvatar           Whether the user set an avatar.
 * @param isOfficial          Whether this is an official Boosty account.
 * @param isBlogger           Profile only: whether the user has a blog.
 * @param blogUrl             Profile only: the user's blog name in {@code boosty.to/<blogUrl>}; sent for bloggers.
 * @param externalApps        Profile only: accounts linked to Boosty by network ({@code discord}, {@code telegram},
 *                            {@code max}).
 * @param canWrite            Whether the blog owner can message the user.
 * @param isBlackListed       Whether the user is on the blog owner's blacklist.
 * @param isBlackListedByUser Profile only: whether the blog owner is on the user's blacklist.
 * @param note                Profile only: the blog owner's note about the user, up to 500 characters.
 * @param subscribed          List only: whether the subscription runs, the same as {@link #status()} being
 *                            {@link SubscriptionStatus#ACTIVE}; see {@link #isSubscribed()} for any source.
 * @param status              Subscription state: coarse from the list, finer from the profile.
 * @param level               Subscription level.
 * @param price               List only: what the subscriber pays per month; may differ from the level price, e.g.
 *                            after a price change or with a {@link #promo()}.
 * @param isFeePaid           List only: whether the subscriber took on Boosty's commission.
 * @param payments            Total income from the user after Boosty's commission: subscription charges, donations,
 *                            post and message purchases.
 * @param refunds             Profile only: total refunded to the user.
 * @param onTime              Start of the subscription.
 * @param offTime             End of access: set once auto-renewal is turned off or the subscription has ended.
 * @param nextPayTime         Next charge, or the end of a trial; {@code 0} when nothing renews.
 * @param lastPayTime         Profile only: last charge; {@code 0} when nothing was charged.
 * @param pauseFrom           List only: start of the last pause.
 * @param pauseTo             List only: end of the last pause.
 * @param promo               List only: promotion the user subscribed with.
 */
public record BoostyUser(
        long id,
        @Nullable String name,
        @Nullable String email,
        @Nullable String avatarUrl,
        boolean hasAvatar,
        boolean isOfficial,
        boolean isBlogger,
        @Nullable String blogUrl,
        @JsonDeserialize(using = LenientDeserializer.class) Map<String, ExternalApp> externalApps,
        boolean canWrite,
        boolean isBlackListed,
        boolean isBlackListedByUser,
        @Nullable String note,
        // Explicit, so that isSubscribed() below is not taken for its accessor
        @JsonProperty("subscribed") boolean subscribed,
        @Nullable SubscriptionStatus status,
        @Nullable SubscriptionLevel level,
        int price,
        boolean isFeePaid,
        double payments,
        double refunds,
        long onTime,
        long offTime,
        long nextPayTime,
        long lastPayTime,
        long pauseFrom,
        long pauseTo,
        @JsonDeserialize(using = LenientDeserializer.class) @Nullable BoostyPromo promo
) {
    /**
     * How long past {@link #nextPayTime()} or {@link #offTime()} a subscription still counts, so that a charge Boosty
     * is still processing does not cut access.
     */
    private static final long GRACE_SECONDS = TimeUnit.DAYS.toSeconds(1);

    public BoostyUser {
        name = BoostyValues.nullIfEmpty(name);
        email = BoostyValues.nullIfEmpty(email);
        avatarUrl = BoostyValues.nullIfEmpty(avatarUrl);
        blogUrl = BoostyValues.nullIfEmpty(blogUrl);
        note = BoostyValues.nullIfEmpty(note);
        externalApps = externalApps == null ? Map.of() : Collections.unmodifiableMap(externalApps);
    }

    /**
     * Whether the user is subscribed now, on any level, from either source.
     * <p>
     * The reported state decides first: {@link #status()}, or {@link #subscribed()} when it is not known; a user
     * without a subscription, like a post buyer, is not subscribed. A state that gives access counts until
     * {@link #nextPayTime()} when the subscription renews or is a trial, or until {@link #offTime()} when its renewal
     * is turned off, both with a day of grace for a charge Boosty is still processing; without either date (the free
     * level) it counts as is.
     *
     * @see #isPaidSubscriber()
     */
    @JsonIgnore
    public boolean isSubscribed() {
        boolean active = this.status != null ? this.status.isActive() : this.subscribed;
        if (!active) {
            return false;
        }

        long deadline = Instant.now().getEpochSecond() - GRACE_SECONDS;

        if (this.nextPayTime > 0) {
            return this.nextPayTime > deadline;
        }

        if (this.offTime > 0) {
            return this.offTime > deadline;
        }

        return true;
    }

    /**
     * Whether the user is subscribed now to a paid level: {@link #isSubscribed()} on a level with a price. A trial on
     * such a level counts; the free level does not.
     *
     * @see #isPaidSubscriber(long...)
     */
    @JsonIgnore
    public boolean isPaidSubscriber() {
        return this.level != null && this.level.price() > 0 && isSubscribed();
    }

    /**
     * Whether the user is subscribed now to one of the given paid levels: {@link #isPaidSubscriber()} on one of them,
     * or on an archived version of one, which keeps those who subscribed at its old price and names the level that
     * replaced it as its {@link SubscriptionLevel#parent()}.
     * <p>
     * A profile sends its level without the parent, so a user read from one matches only by the id of their own level;
     * read them from the subscriber list to match archived levels.
     *
     * @param levelIds Levels to accept; every paid level when empty.
     */
    public boolean isPaidSubscriber(long... levelIds) {
        if (!isPaidSubscriber()) {
            return false;
        }

        if (levelIds.length == 0) {
            return true;
        }

        for (SubscriptionLevel level = this.level; level != null; level = level.parent()) {
            long id = level.id();
            if (LongStream.of(levelIds).anyMatch(levelId -> levelId == id)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public @NotNull String toString() {
        return String.format("BoostyUser{%s - %s (%s)}", this.name, this.email, this.id);
    }
}
