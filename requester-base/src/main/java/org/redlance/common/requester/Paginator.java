package org.redlance.common.requester;

public interface Paginator<T> {
    T data();
    String error();

    int offset();
    boolean done();
}
