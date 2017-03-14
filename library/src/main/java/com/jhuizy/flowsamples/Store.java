package com.jhuizy.flowsamples;

import io.reactivex.Observable;

public interface Store<A, S> extends Dispatcher<A>, StateHolder<S> {

    Observable<S> changes();

}
