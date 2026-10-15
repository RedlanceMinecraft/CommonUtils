package org.redlance.common.requester.boosty.paginators;

import org.jetbrains.annotations.Nullable;
import org.redlance.common.requester.Paginator;
import org.redlance.common.requester.boosty.BoostyErrorResponse;

import java.io.IOException;

/**
 * Page in the format of post sales: {@code {"data": ..., "extra": {"offset", "total"}}}.
 * <p>
 * Errors come as {@code {"error", "error_description"}} instead, see {@link BoostyErrorResponse}.
 *
 * @param <T>              Type of the page content.
 * @param data             Content of the page.
 * @param extra            Paging state.
 * @param error            Error code, e.g. {@code unauthorized}, {@code forbidden} or {@code blog_not_found}.
 * @param errorDescription Readable error text.
 */
public record BoostyPaginator<T>(
        @Nullable T data,
        @Nullable Extra extra,
        @Nullable String error,
        @Nullable String errorDescription
) implements Paginator<T>, BoostyErrorResponse {
    @Override
    public int offset() {
        return this.extra == null ? 0 : this.extra.offset();
    }

    @Override
    public boolean done() {
        return this.extra == null || this.extra.isLast() || this.extra.offset() >= this.extra.total();
    }

    @Override
    public IOException failure() {
        return failure("Boosty sent no page");
    }

    /**
     * Paging state of a page.
     *
     * @param offset Offset of the next page: the requested offset plus the limit. Boosty sends it as a string.
     * @param total  Number of items across all pages.
     * @param isLast Whether this is the last page; not every list sends it.
     */
    public record Extra(int offset, int total, boolean isLast) {
    }
}
