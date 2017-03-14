package com.jhuizy.flowsamples

import android.util.Log
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

object Flow {
    
    private data class CreateStore<A, S>(val stateHolder: StateHolder<S>, val dispatcher: Dispatcher<A>, val changes: Observable<S>) : Store<A, S> {
        override fun dispatch(action: A) {
            dispatcher.dispatch(action)
        }

        override fun getState(): S {
            return stateHolder.state
        }

        override fun changes(): Observable<S> {
            return changes
        }
    }

    fun <A, S> middlewares(middlewares: List<Middleware<A, S>>): StoreCreator<A, S> {

        return StoreCreator { initialState, reducer ->

            val store = DefaultStoreCreator<A, S>().create(initialState, reducer)

            val dispatcher = middlewares.reversed()
                    .map { middleware -> { stateHolder: StateHolder<S> -> { next: Dispatcher<A> -> middleware.apply(stateHolder, next) } } }
                    .map { middelware -> middelware.invoke(store.stateHolder) }
                    .foldRight(store.dispatcher) { m, dispatcher -> m.invoke(dispatcher) }

            store.copy(dispatcher = dispatcher)
        }
    }

    fun <A, S> default(): StoreCreator<A, S> = DefaultStoreCreator<A, S>()

    private class DefaultStoreCreator<A, S> : StoreCreator<A, S> {
        override fun create(initialState: S, reducer: Reducer<A, S>): CreateStore<A, S> {
            var state = initialState
            val stateHolder = StateHolder { state }

            val subject: Subject<A> = PublishSubject.create<A>().toSerialized()
            val changes: Observable<S> = subject.toFlowable(BackpressureStrategy.BUFFER)
                    .toObservable()
                    .map { action -> reducer.reduce(state, action) }
                    .doOnNext { nextState -> state = nextState }

            val dispatcher = Dispatcher<A> { action -> subject.onNext(action) }

            return CreateStore(stateHolder, dispatcher, changes)
        }
    }

    class LoggerMiddleware<A, S> : Middleware<A, S> {
        override fun apply(stateHolder: StateHolder<S>, nextDispatcher: Dispatcher<A>): Dispatcher<A> {
            return Dispatcher { action ->
                val TAG = "LoggerMiddleware"
                Log.v(TAG, "redux action --> $action")
                nextDispatcher.dispatch(action)
                Log.v(TAG, "redux state <-- ${stateHolder.state}")
            }
        }
    }
}

