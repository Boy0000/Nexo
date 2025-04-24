package com.nexomc.nexo.compatibilities.worldedit

import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.event.extent.EditSessionEvent
import com.sk89q.worldedit.util.eventbus.Subscribe

class WorldEditHandlers {

    @Subscribe
    @Suppress("unused")
    fun EditSessionEvent.onEditSession() {
        if (stage == EditSession.Stage.BEFORE_CHANGE) {
            if (WrappedWorldEdit.isFaweEnabled) extent.addPostProcessor(NexoCustomBlocksProcessor(BukkitAdapter.adapt(world)))
            else extent = NexoWorldEditExtent(extent, BukkitAdapter.adapt(world))
        }
    }
}
