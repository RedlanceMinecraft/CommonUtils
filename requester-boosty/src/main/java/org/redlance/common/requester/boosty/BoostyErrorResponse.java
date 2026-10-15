package org.redlance.common.requester.boosty;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Response that carries an error instead of its content: {@code {"error": ..., "error_description": ...}}.
 */
public interface BoostyErrorResponse {
    /**
     * Error code, e.g. {@code unauthorized}, {@code forbidden}, {@code blog_not_found} or {@code user_not_found}.
     */
    @Nullable String error();

    /**
     * Readable error text.
     */
    @JsonProperty("error_description")
    @Nullable String errorDescription();

    /**
     * @param missing What the response lacks, for the message.
     * @return The error of this response as an exception.
     */
    default BoostyRequestException failure(String missing) {
        return new BoostyRequestException(missing, error(), errorDescription());
    }
}
