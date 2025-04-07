package com.nexomc.nexo.mechanics.custom_block.chorusblock

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoItems
import org.bukkit.entity.FallingBlock
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class ChorusBlockListener : Listener {

    @EventHandler
    fun EntityRemoveFromWorldEvent.onFallingBlockLandOnCarpet() {
        val fallingBlock = entity as? FallingBlock ?: return
        val mechanic = NexoBlocks.chorusBlockMechanic(fallingBlock.blockData) ?: return
        if (NexoBlocks.customBlockMechanic(fallingBlock.location) == mechanic) return

        val itemStack = NexoItems.itemFromId(mechanic.itemID)!!.build()
        fallingBlock.dropItem = false
        fallingBlock.world.dropItemNaturally(fallingBlock.location.toCenterLocation().subtract(0.0, 0.25, 0.0), itemStack)
    }
}