package com.nexomc.nexo.api.events.resourcepack

import com.nexomc.nexo.api.NexoPack
import com.nexomc.nexo.pack.creative.NexoPackReader
import com.nexomc.nexo.utils.printOnFailure
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.base.Writable
import java.io.File

class NexoPrePackGenerateEvent(val resourcePack: ResourcePack) : Event() {

    fun addResourcePack(resourcePack: ResourcePack): Boolean {
        return runCatching {
            NexoPack.mergePack(this.resourcePack, resourcePack)
        }.printOnFailure().getOrNull() != null
    }

    fun addResourcePack(resourcePack: File): Boolean {
        return runCatching {
            NexoPack.mergePack(this.resourcePack, NexoPackReader.INSTANCE.readFile(resourcePack))
        }.printOnFailure().getOrNull() != null
    }

    fun addUnknownFile(path: String, data: ByteArray): Boolean {
        return runCatching {
            this.resourcePack.unknownFile(path, Writable.bytes(data))
        }.printOnFailure().getOrNull() != null
    }

    override fun getHandlers() = handlerList

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
