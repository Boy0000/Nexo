package com.nexomc.nexo.compatibilities.worldedit

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.event.extent.EditSessionEvent
import com.sk89q.worldedit.util.eventbus.Subscribe
import org.bukkit.Bukkit

class WorldEditHandlers(register: Boolean) {
    init {
        if (register) WorldEdit.getInstance().eventBus.register(this)
        else WorldEdit.getInstance().eventBus.unregister(this)
    }


    @Subscribe
    @Suppress("unused")
    fun EditSessionEvent.onEditSession() {
        val world = world?.name?.let(Bukkit::getWorld) ?: return
        extent = NexoWorldEditExtent(extent, world)
    }
}
