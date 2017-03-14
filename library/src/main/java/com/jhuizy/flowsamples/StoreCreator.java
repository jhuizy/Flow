package com.jhuizy.flowsamples;

public interface StoreCreator<A, S> {
    Store<A, S> create(S initialState, Reducer<A, S> reducer);
}
