package org.redlance.common.requester.boosty.obj.post;

import org.jetbrains.annotations.NotNull;

/**
 * @param id uuid-like post id.
 * @param title Post title.
 */
public record PostObj(String id, String title) {
    @Override
    public @NotNull String toString() {
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
