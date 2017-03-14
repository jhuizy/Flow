package com.jhuizy.flowsamples

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.LinearLayout
import io.reactivex.Completable
import trikita.anvil.Anvil
import trikita.anvil.DSL.*
import trikita.anvil.DSL.linearLayout
import trikita.anvil.RenderableView
import java.util.concurrent.TimeUnit

data class CountState(val count: Int, val loading: Boolean)

enum class CountAction { StartIncrement, Increment }

class CountReducer : Flow.Reducer<CountAction, CountState> {
    override fun reduce(state: CountState, action: CountAction): CountState {
        return when(action) {
            CountAction.Increment -> state.copy(loading = false, count = state.count + 1)
            CountAction.StartIncrement -> state.copy(loading = true)
        }
    }
}

class CountServiceMiddleware : Flow.Middleware<CountAction, CountState> {
    override fun apply(stateHolder: Flow.StateHolder<CountState>, nextDispatcher: Flow.Dispatcher<CountAction>): Flow.Dispatcher<CountAction> {
        return Flow.Dispatcher.Create { action ->
            when(action) {
                CountAction.StartIncrement -> {
                    Completable.timer(2, TimeUnit.SECONDS).subscribe { nextDispatcher.dispatch(CountAction.Increment) }
                }
            }

            nextDispatcher.dispatch(action)
        }
    }
}

class CountActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val store = Flow.middlewares(listOf(CountServiceMiddleware())).createStore(CountState(0, false), CountReducer())
        store.changes.subscribe { Anvil.render() }

        setContentView(CountView(this, store))
    }

}

class CountView(context: Context, val store: Flow.Store<CountAction, CountState>) : RenderableView(context) {

    override fun view() {
        linearLayout() {
            size(MATCH, MATCH)
            orientation(LinearLayout.VERTICAL)

            textView() {
                size(MATCH, WRAP)
                text("Counter = ${store.stateHolder.state().count}")
            }

            button() {
                size(MATCH, WRAP)
                text(if (store.stateHolder.state().loading) "Loading" else "Increment")
                enabled(!store.stateHolder.state().loading)
                onClick { store.dispatcher.dispatch(CountAction.StartIncrement) }
            }
        }
    }
}