package org.redlance.common.requester.boosty.obj;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Reads values the way Boosty sends them.
 */
@ApiStatus.Internal
public final class BoostyValues {
    private BoostyValues() {
    }

    /**
     * @return The constant named {@code value} in any case, or {@code fallback} for a value this wrapper does not
     * know, so that a new value from Boosty does not fail the whole response.
     */
    public static <E extends Enum<E>> E parse(E[] constants, String value, E fallback) {
        for (E constant : constants) {
            if (constant.name().equalsIgnoreCase(value)) {
                return constant;
            }
        }

        return fallback;
    }

    /**
     * @return {@code null} for an empty text, which Boosty sends where a value is missing.
     */
    public static @Nullable String nullIfEmpty(@Nullable String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
