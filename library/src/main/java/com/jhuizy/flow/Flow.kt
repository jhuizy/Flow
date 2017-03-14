package com.jhuizy.flow

import android.util.Log
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

object Flow {

    interface Dispatcher<A> {
        fun dispatch(action: A)

        class Create<A>(val f: (A) -> Unit) : Dispatcher<A> {
            override fun dispatch(action: A) {
                f(action)
            }
        }
    }

    interface StateHolder<S> {
        fun state(): S

        class Create<S>(val f: () -> S) : StateHolder<S> {
            override fun state(): S {
                return f()
            }
        }
    }

    interface Reducer<A, S> {
        fun reduce(state: S, action: A): S

        class Create<A, S>(val f: (S, A) -> S) : Reducer<A, S> {
            override fun reduce(state: S, action: A): S {
                return f(state, action)
            }
        }
    }

    interface Middleware<A, S> {
        fun apply(stateHolder: StateHolder<S>, nextDispatcher: Dispatcher<A>): Dispatcher<A>

        class Create<A, S>(val f: (StateHolder<S>, Dispatcher<A>) -> Dispatcher<A>) : Middleware<A, S> {
            override fun apply(stateHolder: StateHolder<S>, nextDispatcher: Dispatcher<A>): Dispatcher<A> {
                return f(stateHolder, nextDispatcher)
            }
        }
    }

    interface StoreCreator<A, S> {
        fun createStore(initialState: S, reducer: Reducer<A, S>): Store<A, S>

        class Create<A, S>(val f: (S, Reducer<A, S>) -> Store<A, S>) : StoreCreator<A, S> {
            override fun createStore(initialState: S, reducer: Reducer<A, S>): Store<A, S> {
                return f(initialState, reducer)
            }
        }
    }

    data class Store<A, S>(val stateHolder: StateHolder<S>, val dispatcher: Dispatcher<A>, val changes: Observable<S>)

    fun <A, S> middlewares(middlewares: List<Middleware<A, S>>): StoreCreator<A, S> {

        return StoreCreator.Create { initialState, reducer ->

            val store = store(initialState, reducer)

            val dispatcher = middlewares.reversed()
                    .map { middleware -> { stateHolder: StateHolder<S> -> { next: Dispatcher<A> -> middleware.apply(stateHolder, next) } } }
                    .map { middelware -> middelware.invoke(store.stateHolder) }
                    .foldRight(store.dispatcher) { m, dispatcher -> m.invoke(dispatcher) }

            store.copy(dispatcher = dispatcher)
        }
    }

    fun <A, S> store(initialState: S, reducer: Reducer<A, S>): Store<A, S> {
        var state = initialState
        val stateHolder = StateHolder.Create { state }

        val subject: Subject<A> = PublishSubject.create<A>().toSerialized()
        val changes: Observable<S> = subject.toFlowable(BackpressureStrategy.BUFFER)
                .toObservable()
                .map { action -> reducer.reduce(state, action) }
                .doOnNext { nextState -> state = nextState }

        val dispatcher = Dispatcher.Create<A> { action -> subject.onNext(action) }

        return Store(stateHolder, dispatcher, changes)
    }

    class LoggerMiddleware<A, S> : Middleware<A, S> {
        override fun apply(stateHolder: StateHolder<S>, nextDispatcher: Dispatcher<A>): Dispatcher<A> {
            return Dispatcher.Create { action ->
                val TAG = "LoggerMiddleware"
                Log.v(TAG, "redux action --> $action")
                nextDispatcher.dispatch(action)
                Log.v(TAG, "redux state <-- ${stateHolder.state()}")
            }
        }
    }
}

