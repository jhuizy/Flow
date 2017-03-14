package com.jhuizy.flowsamples;

public interface Dispatcher<A> {
    void dispatch(A action);
}
