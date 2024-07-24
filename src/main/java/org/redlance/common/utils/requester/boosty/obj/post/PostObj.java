package org.redlance.common.utils.requester.boosty.obj.post;

import org.redlance.common.utils.requester.boosty.obj.user.BoostyUser;

/**
 * @param id uuid-like post id.
 * @param title Post title.
 */
public record PostObj(String id, String title) {
    @Override
    public String toString() {
        return "Post{" + this.title + "}";
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PostObj otherPost)) {
            return false;
        }

        return this.id.equals(otherPost.id);
    }
}
