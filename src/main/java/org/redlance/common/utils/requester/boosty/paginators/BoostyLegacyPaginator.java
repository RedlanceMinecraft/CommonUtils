package org.redlance.common.utils.requester.boosty.paginators;

import org.redlance.common.utils.requester.Paginator;

public record BoostyLegacyPaginator<T>(T data, int limit, int offset, int total) implements Paginator<T> {
    @Override
    public boolean done() {
        return offset() >= total();
    }
}
