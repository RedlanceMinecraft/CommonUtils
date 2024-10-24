package org.redlance.common.utils.requester.boosty.obj.user;

public class SubscriptionLevel { // TODO full impl: currencyPrices, externalApps
    /**
     * Whether the level is deleted.
     */
    public boolean deleted;

    /**
     * Unique level id.
     */
    public int id;

    /**
     * Whether the level is archived.
     */
    public boolean isArchived;

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
