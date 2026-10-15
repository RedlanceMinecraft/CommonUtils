package org.redlance.common.requester.boosty.paginators;

import org.jetbrains.annotations.Nullable;
import org.redlance.common.requester.Paginator;
import org.redlance.common.requester.boosty.BoostyErrorResponse;

import java.io.IOException;

/**
 * Page in the format of the subscriber list: {@code {"data": ..., "limit", "offset", "total"}}.
 * <p>
 * Errors come as {@code {"error", "error_description"}} instead, see {@link BoostyErrorResponse}.
 *
 * @param <T>              Type of the page content.
 * @param data             Content of the page.
 * @param limit            Requested page size.
 * @param offset           Offset of the next page: the requested offset plus the number of items returned.
 * @param total            Number of items across all pages.
 * @param error            Error code, e.g. {@code unauthorized}, {@code forbidden} or {@code blog_not_found}.
 * @param errorDescription Readable error text.
 */
public record BoostyLegacyPaginator<T>(
        @Nullable T data,
        int limit,
        int offset,
        int total,
        @Nullable String error,
        @Nullable String errorDescription
) implements Paginator<T>, BoostyErrorResponse {
    @Override
    public boolean done() {
        return this.offset >= this.total;
    }

    @Override
    public IOException failure() {
        return failure("Boosty sent no page");
    }
}
