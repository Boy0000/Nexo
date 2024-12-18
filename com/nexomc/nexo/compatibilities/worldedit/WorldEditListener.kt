package com.nexomc.nexo.compatibilities.worldedit

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoItems
import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class WorldEditListener : Listener {
    @EventHandler
    fun AsyncTabCompleteEvent.onTabComplete() {
        val args = buffer.split(" ")
        if (!buffer.startsWith("//") || args.isEmpty()) return

        val ids = nexoBlockIDs.filter { id: String -> ("nexo:$id").startsWith(args[args.size - 1]) }
            .map { "nexo:$it" }.toMutableList().apply { this += completions }
        setCompletions(ids)
    }

    companion object {
        private val nexoBlockIDs = NexoItems.entries().map { entry -> entry.key.lowercase() }.filter(NexoBlocks::isCustomBlock)
    }
}
