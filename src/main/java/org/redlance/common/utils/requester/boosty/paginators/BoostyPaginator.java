package org.redlance.common.utils.requester.boosty.paginators;

import com.google.gson.JsonObject;
import org.redlance.common.utils.requester.Paginator;

public record BoostyPaginator<T>(T data, JsonObject extra) implements Paginator<T> {
    @Override
    public int offset() {
        return extra().get("offset").getAsInt();
    }

    @Override
    public boolean done() {
        return offset() >= extra().get("total").getAsInt();
    }
}
