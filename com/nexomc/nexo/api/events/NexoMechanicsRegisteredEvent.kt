package com.nexomc.nexo.api.events

import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * This event is called when all native mechanics are registered.
 * Useful to register your own mechanics, and re-register on reloads.
 */
class NexoMechanicsRegisteredEvent : Event() {
    override fun getHandlers() = handlerList

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
