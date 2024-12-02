package org.redlance.common.utils.requester;

public interface Paginator<T> {
    T data();

    int offset();
    boolean done();
}
