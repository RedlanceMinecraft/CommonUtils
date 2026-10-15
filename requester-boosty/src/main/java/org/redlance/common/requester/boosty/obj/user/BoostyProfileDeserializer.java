package org.redlance.common.requester.boosty.obj.user;

import org.jetbrains.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

/**
 * Reads a subscriber profile into a {@link BoostyUser}, flattening it the way the subscriber list sends users.
 * <p>
 * Boosty nests the profile:
 * <pre>{@code
 * {
 *   "user": {"id", "name", "email", "avatarUrl", "hasAvatar", "isOfficial", "isBlogger", "blogUrl", "externalApps"},
 *   "subscription": {"subscriptionLevel", "status", "onTime", "offTime", "nextPayTime", "lastPayTime"},
 *   "finance": {"payments", "refunds"},
 *   "note", "canWrite", "isBlackListed", "isBlackListedByUser"
 * }
 * }</pre>
 * The user is taken as it is, and only the fields above are added from the rest, {@code subscriptionLevel} as
 * {@link BoostyUser#level()}. A user who never subscribed has no {@code subscription}. A profile without a user is
 * read as {@code null}.
 * <p>
 * Apply it to the property that holds the profile:
 * {@code @JsonDeserialize(using = BoostyProfileDeserializer.class) BoostyUser profile}.
 */
public class BoostyProfileDeserializer extends ValueDeserializer<BoostyUser> {
    private static final List<String> PROFILE_FIELDS = List.of("note", "canWrite", "isBlackListed", "isBlackListedByUser");
    private static final List<String> FINANCE_FIELDS = List.of("payments", "refunds");

    /**
     * Subscription fields by the {@link BoostyUser} component they fill.
     */
    private static final Map<String, String> SUBSCRIPTION_FIELDS = Map.of(
            "subscriptionLevel", "level",
            "status", "status",
            "onTime", "onTime",
            "offTime", "offTime",
            "nextPayTime", "nextPayTime",
            "lastPayTime", "lastPayTime"
    );

    @Override
    public @Nullable BoostyUser deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonNode profile = ctxt.readTree(p);
        if (!(profile.get("user") instanceof ObjectNode user)) {
            return null;
        }

        // The tree is this read's own, so the user takes the fields in place
        PROFILE_FIELDS.forEach(field -> copy(profile, field, user, field));
        FINANCE_FIELDS.forEach(field -> copy(profile.path("finance"), field, user, field));

        // Earlier versions of the API named the subscription "level"
        JsonNode subscription = profile.has("subscription") ? profile.get("subscription") : profile.path("level");
        SUBSCRIPTION_FIELDS.forEach((from, to) -> copy(subscription, from, user, to));

        return ctxt.readTreeAsValue(user, BoostyUser.class);
    }

    private static void copy(JsonNode source, String from, ObjectNode target, String to) {
        JsonNode value = source.get(from);
        if (value != null) {
            target.set(to, value);
        }
    }
}
