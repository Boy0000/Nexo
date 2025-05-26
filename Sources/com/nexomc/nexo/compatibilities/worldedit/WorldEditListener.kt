package com.nexomc.nexo.compatibilities.worldedit

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent
import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoItems
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class WorldEditListener : Listener {
    @EventHandler
    fun AsyncTabCompleteEvent.onTabComplete() {
        val arg = buffer.takeIf { it.startsWith("//") }?.substringAfterLast(" ") ?: return
        completions = nexoBlockIDs.filter { it.contains(arg) }.plus(completions)
    }

    private val nexoBlockIDs by lazy { NexoItems.unexcludedItemNames().filter(NexoBlocks::isCustomBlock).map { "nexo:$it" } }
}
