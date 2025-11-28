package org.redlance.common.utils.requester.boosty.paginators;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.redlance.common.utils.requester.Paginator;

public record BoostyPaginator<T>(T data, ObjectNode extra) implements Paginator<T> {
    @Override
    public int offset() {
        return extra().get("offset").intValue();
    }

    @Override
    public boolean done() {
        return offset() >= extra().get("total").intValue();
    }
}
