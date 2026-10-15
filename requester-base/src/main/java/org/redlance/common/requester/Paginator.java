package org.redlance.common.requester;

import java.io.IOException;

public interface Paginator<T> {
    T data();
    String error();

    int offset();
    boolean done();

    /**
     * The {@link #error()} of a failed page, or the lack of {@link #data()}, as an exception.
     */
    default IOException failure() {
        return new IOException(error() != null ? error() : "The page has no data");
    }
}
