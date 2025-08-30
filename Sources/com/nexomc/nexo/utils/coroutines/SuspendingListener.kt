package com.nexomc.nexo.utils.coroutines

import org.bukkit.event.Event
import org.bukkit.event.Listener
import kotlin.coroutines.CoroutineContext

open class SuspendingListener : Listener {
    open val dispatchers: MutableMap<Class<out Event>, (Event) -> CoroutineContext> = mutableMapOf()

    inline fun <reified T : Event> registerDispatcher(noinline dispatcher: (T) -> CoroutineContext) {
        dispatchers[T::class.java] = { event: Event ->
            require(event is T) { "Event must be of type ${T::class.java.simpleName}" }
            dispatcher(event)
        }
    }

    inline fun <reified T : Event> registerDispatcher(context: CoroutineContext) {
        dispatchers[T::class.java] = { event: Event ->
            require(event is T) { "Event must be of type ${T::class.java.simpleName}" }
            context
        }
    }
}