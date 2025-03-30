package com.nexomc.nexo.compatibilities.worldedit

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent
import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoItems
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class WorldEditListener : Listener {
    @EventHandler
    fun AsyncTabCompleteEvent.onTabComplete() {
        val args = completions.takeIf { buffer.startsWith("//") }?.takeUnless { it.isEmpty() } ?: return
        completions = nexoBlockIDs.filter { args.any(it::contains) }.plus(completions)
    }

    companion object {
        private val nexoBlockIDs by lazy { NexoItems.itemNames().filter(NexoBlocks::isCustomBlock).map { "nexo:$it" } }
    }
}
