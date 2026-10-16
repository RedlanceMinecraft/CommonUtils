package org.redlance.common.requester.boosty.obj.donation;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.jetbrains.annotations.NotNull;
import org.redlance.common.requester.boosty.obj.BoostyValues;
import org.redlance.common.requester.boosty.obj.user.BoostyUser;

/**
 * Donation to the blog.
 *
 * @param id        Unique donation id.
 * @param type      What the donation went to.
 * @param targetId  Id of what the donation went to: the goal for {@link Type#TARGET}, the post for {@link Type#POST};
 *                  {@code 0} for {@link Type#AUTHOR}.
 * @param amount    Income from the donation in rubles, converted from the donor's currency, after Boosty's commission;
 *                  see {@link #isFeePaid()}.
 * @param isFeePaid Whether the donor took on Boosty's commission.
 * @param createdAt Donation time, Unix seconds.
 * @param bloggerId Id of the blog owner.
 * @param user      The donor; Boosty sends only the identity and {@link BoostyUser#canWrite()}.
 */
public record Donation(
        long id,
        Type type,
        long targetId,
        double amount,
        boolean isFeePaid,
        long createdAt,
        long bloggerId,
        BoostyUser user
) {
    @Override
    public @NotNull String toString() {
        return "Donation{" + this.id + " (" + this.type + "), user=" + this.user + "}";
    }

    /**
     * What a donation went to.
     */
    public enum Type {
        /**
         * The author, from their profile.
         */
        AUTHOR,

        /**
         * A goal of the blog, see {@link Donation#targetId()}.
         */
        TARGET,

        /**
         * A post, see {@link Donation#targetId()}.
         */
        POST,

        /**
         * A message in a dialog with the author.
         */
        DIALOG,

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
}
