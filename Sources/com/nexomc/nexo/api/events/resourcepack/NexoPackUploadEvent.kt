package com.nexomc.nexo.api.events.resourcepack

import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class NexoPackUploadEvent(val hash: String, val url: String) : Event() {

    override fun getHandlers() = handlerList

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
