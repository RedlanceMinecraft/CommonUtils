package org.redlance.common.requester.boosty.obj.user;

import com.fasterxml.jackson.annotation.JsonAlias;

public class SubscriptionLevel { // TODO full impl: currencyPrices, externalApps, createdAt, data, ownerId
    /**
     * Whether the level is deleted.
     */
    @JsonAlias(value = {"isDeleted", "deleted"})
    public boolean isDeleted;

    /**
     * Unique level id.
     */
    public long id;

    /**
     * Whether the level is archived.
     */
    public boolean isArchived;

    /**
     * Whether the level is limited.
     */
    public boolean isLimited;

    /**
     * Whether the level is hidden.
     */
    public boolean isHidden;

    /**
     * Level name.
     */
    public String name;

    /**
     * The price for which the user purchased the level.
     */
    public long price;

    @Override
    public String toString() {
        return String.format("SubscriptionLevel{%s (%s)}", name, price);
    }
}
