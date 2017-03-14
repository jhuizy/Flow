package com.jhuizy.flowsamples;

public interface Reducer<A, S> {
    S reduce(S state, A action);
}
