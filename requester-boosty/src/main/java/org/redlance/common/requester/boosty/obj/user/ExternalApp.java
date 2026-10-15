package org.redlance.common.requester.boosty.obj.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redlance.common.requester.boosty.obj.BoostyValues;

/**
 * Account in another network that a user linked to Boosty, see {@link BoostyUser#externalApps()}.
 *
 * @param id         Account id in the network (a snowflake for Discord); {@code null} when not linked.
 * @param username   Account name in the network; {@code null} when not linked.
 * @param hasAccount Whether the account is linked.
 * @param isJoined   Whether the user joined the blog's chat in the network; sent for {@code telegram} and
 *                   {@code max} only.
 */
public record ExternalApp(@Nullable String id, @Nullable String username, boolean hasAccount, boolean isJoined) {
    public ExternalApp {
        id = BoostyValues.nullIfEmpty(id);
        username = BoostyValues.nullIfEmpty(username);
    }

    @Override
    public @NotNull String toString() {
        return "ExternalApp{" + this.username + "}";
    }
}
