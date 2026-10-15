package org.redlance.common.requester.boosty;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Indicates that Boosty answered a request with an error, {@code {"error": ..., "error_description": ...}}, or
 * without the expected content.
 */
public final class BoostyRequestException extends IOException {
    private final @Nullable String error;
    private final @Nullable String errorDescription;

    /**
     * @param message          What was expected from the response.
     * @param error            Error code from the response, if any.
     * @param errorDescription Error text from the response, if any.
     */
    public BoostyRequestException(String message, @Nullable String error, @Nullable String errorDescription) {
        super(message + describe(error, errorDescription));
        this.error = error;
        this.errorDescription = errorDescription;
    }

    private static String describe(@Nullable String error, @Nullable String errorDescription) {
        if (error == null) {
            return errorDescription == null ? "" : ": " + errorDescription;
        }

        return ": " + error + (errorDescription == null ? "" : " (" + errorDescription + ")");
    }

    /**
     * Error code, e.g. {@code unauthorized}, {@code forbidden}, {@code blog_not_found} or {@code user_not_found};
     * {@code null} when the response had none.
     */
    public @Nullable String error() {
        return this.error;
    }

    /**
     * Readable error text, e.g. {@code User not found}.
     */
    public @Nullable String errorDescription() {
        return this.errorDescription;
    }
}
