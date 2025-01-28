package com.nexomc.nexo.mechanics.custom_block.noteblock

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoItems
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import com.nexomc.nexo.utils.BlockHelpers.toCenterLocation
import org.bukkit.entity.FallingBlock
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class NoteBlockMechanicPaperListener : Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    fun EntityRemoveFromWorldEvent.onFallingBlockLandOnCarpet() {
        val fallingBlock = (entity as? FallingBlock)?.takeIf { it.persistentDataContainer.has(NoteBlockMechanic.FALLING_KEY) } ?: return
        val mechanic = NexoBlocks.noteBlockMechanic(fallingBlock.blockData)?.let { it.directional?.parentMechanic ?: it }?.takeIf { it.isFalling() } ?: return
        if (NexoBlocks.customBlockMechanic(fallingBlock.location) == mechanic) return

        val itemStack = NexoItems.itemFromId(mechanic.itemID)!!.build()
        fallingBlock.dropItem = false
        fallingBlock.world.dropItemNaturally(toCenterLocation(fallingBlock.location).subtract(0.0, 0.25, 0.0), itemStack)
    }
}
