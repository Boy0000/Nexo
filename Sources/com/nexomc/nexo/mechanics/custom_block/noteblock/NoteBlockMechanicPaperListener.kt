package com.nexomc.nexo.mechanics.custom_block.noteblock

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoItems
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import org.bukkit.entity.FallingBlock
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class NoteBlockMechanicPaperListener : Listener {
    @EventHandler
    fun EntityRemoveFromWorldEvent.onFallingBlockLandOnCarpet() {
        val fallingBlock = entity as? FallingBlock ?: return
        val mechanic = NexoBlocks.noteBlockMechanic(fallingBlock.blockData)?.let { it.directional?.parentMechanic ?: it } ?: return
        if (NexoBlocks.customBlockMechanic(fallingBlock.location) == mechanic) return

        val itemStack = NexoItems.itemFromId(mechanic.itemID)!!.build()
        fallingBlock.dropItem = false
        fallingBlock.world.dropItemNaturally(fallingBlock.location, itemStack)
    }
}
