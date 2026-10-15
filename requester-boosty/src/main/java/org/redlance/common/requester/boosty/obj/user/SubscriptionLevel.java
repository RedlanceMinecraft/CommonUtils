package org.redlance.common.requester.boosty.obj.user;

import com.fasterxml.jackson.annotation.JsonAlias;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redlance.common.requester.boosty.obj.BoostyValues;
import org.redlance.common.requester.boosty.obj.LenientDeserializer;
import org.redlance.common.requester.boosty.obj.post.ContentBlock;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Subscription level of a blog.
 * <p>
 * The subscriber list sends the full level. The subscriber profile sends a short one: id, name, prices, the
 * deleted and archived flags, and {@link #externalApps()}, which only it has. A missing component is {@code 0},
 * {@code false}, {@code null} or empty. The prices, the parent, the description and the integrations are left out
 * when Boosty sends them in a shape this wrapper does not expect.
 *
 * @param id             Unique level id.
 * @param name           Level name.
 * @param price          Price per month in rubles; {@code 0} for the free level.
 * @param currencyPrices Price per month by currency; Boosty sends {@link Currency#RUB}, {@link Currency#USD} and
 *                       {@link Currency#EUR}.
 * @param ownerId        Id of the blog owner.
 * @param createdAt      Creation time, Unix seconds.
 * @param deleted        Whether the author deleted the level.
 * @param isArchived     Whether the level is archived: no longer offered, but kept for those subscribed to it.
 * @param archivedAt     Archiving time, Unix seconds; sent for an archived level that has a {@link #parent()}.
 * @param isHidden       Whether the level is hidden from the blog page.
 * @param isLimited      Whether the number of subscribers is limited.
 * @param parent         For an archived level, the level that replaced it, e.g. after a price change; Boosty sends
 *                       only its id, name and prices.
 * @param data           Level description.
 * @param externalApps   Profile only: integrations of the level by network ({@code discord}, {@code telegram},
 *                       {@code max}).
 */
public record SubscriptionLevel(
        long id,
        @Nullable String name,
        int price,
        @JsonDeserialize(using = LenientDeserializer.class) Map<Currency, Double> currencyPrices,
        long ownerId,
        long createdAt,
        @JsonAlias("isDeleted") boolean deleted,
        boolean isArchived,
        long archivedAt,
        boolean isHidden,
        boolean isLimited,
        @JsonDeserialize(using = LenientDeserializer.class) @Nullable SubscriptionLevel parent,
        @JsonDeserialize(using = LenientDeserializer.class) List<ContentBlock> data,
        @JsonDeserialize(using = LenientDeserializer.class) Map<String, ExternalAppConfig> externalApps
) {
    public SubscriptionLevel {
        name = BoostyValues.nullIfEmpty(name);
        currencyPrices = Currency.knownPrices(currencyPrices);
        data = data == null ? List.of() : Collections.unmodifiableList(data);
        externalApps = externalApps == null ? Map.of() : Collections.unmodifiableMap(externalApps);
    }

    @Override
    public @NotNull String toString() {
        return String.format("SubscriptionLevel{%s (%s)}", this.name, this.price);
    }

    /**
     * Integration of a level with another network.
     *
     * @param isConfigured Whether the author set the integration up.
     * @param data         Integration settings; sent for {@code discord}.
     */
    public record ExternalAppConfig(boolean isConfigured, @Nullable Data data) {
        /**
         * Settings of an integration.
         *
         * @param role Discord role that subscribers of the level get.
         */
        public record Data(@Nullable Role role) {
        }

        /**
         * Discord role.
         *
         * @param id   Role snowflake; {@code null} when not configured.
         * @param name Role name; {@code null} when not configured.
         */
        public record Role(@Nullable String id, @Nullable String name) {
            public Role {
                id = BoostyValues.nullIfEmpty(id);
                name = BoostyValues.nullIfEmpty(name);
            }
        }
    }
}
