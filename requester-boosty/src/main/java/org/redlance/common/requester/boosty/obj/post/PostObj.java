package org.redlance.common.requester.boosty.obj.post;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redlance.common.requester.boosty.obj.BoostyValues;
import org.redlance.common.requester.boosty.obj.LenientDeserializer;
import org.redlance.common.requester.boosty.obj.user.BoostyPromo;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * Post as a post sale refers to it.
 *
 * @param id    Uuid of the post.
 * @param title Post title.
 * @param promo Promotion on the post; unset in observed responses, and left out when Boosty sends it in a shape this
 *              wrapper does not expect.
 */
public record PostObj(
        @Nullable String id,
        @Nullable String title,
        @JsonDeserialize(using = LenientDeserializer.class) @Nullable BoostyPromo promo
) {
    public PostObj {
        id = BoostyValues.nullIfEmpty(id);
        title = BoostyValues.nullIfEmpty(title);
    }

    @Override
    public @NotNull String toString() {
        return "Post{" + this.title + "}";
    }
}
