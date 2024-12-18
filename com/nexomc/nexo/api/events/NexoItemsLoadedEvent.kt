package com.nexomc.nexo.api.events

import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class NexoItemsLoadedEvent : Event() {
    override fun getHandlers() = handlerList

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
