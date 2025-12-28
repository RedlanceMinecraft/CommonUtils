package org.redlance.common.utils.requester;

public interface Paginator<T> {
    T data();
    String error();

    int offset();
    boolean done();
}
