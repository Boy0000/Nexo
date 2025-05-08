package com.nexomc.nexo.compatibilities.worldedit

import com.nexomc.nexo.configs.Settings
import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.event.extent.EditSessionEvent
import com.sk89q.worldedit.util.eventbus.Subscribe

class WorldEditHandlers {

    @Subscribe
    @Suppress("unused")
    fun EditSessionEvent.onEditSession() {
        if (!Settings.WORLDEDIT_FURNITURE.toBool() && !Settings.WORLDEDIT_CUSTOM_BLOCKS.toBool()) return
        val world = BukkitAdapter.adapt(world ?: return) ?: return
        if (stage == EditSession.Stage.BEFORE_CHANGE) {
            if (WrappedWorldEdit.isFaweEnabled) extent.addPostProcessor(NexoCustomBlocksProcessor(world))
            else extent = NexoWorldEditExtent(extent, world)
        }
    }
}
