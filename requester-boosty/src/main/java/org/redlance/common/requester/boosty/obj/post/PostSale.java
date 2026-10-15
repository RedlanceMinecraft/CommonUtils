package org.redlance.common.requester.boosty.obj.post;

import org.jetbrains.annotations.NotNull;
import org.redlance.common.requester.boosty.obj.user.BoostyUser;

/**
 * Purchase of a paid post.
 *
 * @param amount    Income from the sale in rubles after Boosty's commission; the full price when
 *                  {@link #isFeePaid()}.
 * @param isFeePaid Whether the buyer took on Boosty's commission.
 * @param createdAt Purchase time, Unix seconds.
 * @param bloggerId Id of the blog owner.
 * @param post      The purchased post.
 * @param user      The buyer; Boosty sends only the identity and {@link BoostyUser#canWrite()}.
 */
public record PostSale(double amount, boolean isFeePaid, long createdAt, long bloggerId, PostObj post, BoostyUser user) {
    @Override
    public @NotNull String toString() {
        return "PostSale{post=" + this.post + ", user=" + this.user + "}";
    }
}
