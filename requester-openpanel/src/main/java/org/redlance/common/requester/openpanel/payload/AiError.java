package org.redlance.common.requester.openpanel.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Failure of the step an {@link AiPayload} describes; its presence marks the event as failed.
 *
 * @param message    What went wrong; sent even when empty.
 * @param code       Error code, e.g. the provider's; up to 200 characters.
 * @param httpStatus HTTP status the provider answered with, from 100 to 599.
 */
public record AiError(
        @JsonInclude(JsonInclude.Include.NON_NULL) String message,
        @Nullable String code,
        @Nullable Integer httpStatus
) {
    public AiError {
        Objects.requireNonNull(message, "message");
    }

    public AiError(String message) {
        this(message, null, null);
    }
}
