package org.redlance.common.utils.requester.boosty.obj.post;

import org.redlance.common.utils.requester.boosty.obj.user.BoostyUser;

public class PostSale {
    /**
     * The price for which the user bought the post.
     */
    public int amount;

    /**
     * Date the post was created.
     */
    public int createdAt;

    /**
     * Id of post creator.
     */
    public int bloggerId;

    /**
     * The post itself.
     */
    public PostObj post;

    /**
     * Buyer of the post.
     */
    public BoostyUser user;

    @Override
    public String toString() {
        return "PostSale{post=" + this.post + ", user=" + this.user + "}";
    }
}
