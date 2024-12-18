package com.nexomc.nexo.mechanics.custom_block.stringblock

import io.papermc.paper.event.entity.EntityInsideBlockEvent
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class StringBlockMechanicPaperListener : Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun EntityInsideBlockEvent.onEnteringTripwire() {
        if (block.type == Material.TRIPWIRE) isCancelled = true
    }
}
