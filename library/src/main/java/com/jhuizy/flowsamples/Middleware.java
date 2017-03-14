package com.jhuizy.flowsamples;

public interface Middleware<A, S> {
    Dispatcher<A> apply(StateHolder<S> stateHolder, Dispatcher<A> nextDispatcher);
}
