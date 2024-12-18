package com.nexomc.nexo.api.events.custom_block.noteblock

import com.nexomc.nexo.api.events.custom_block.NexoBlockPlaceEvent
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteBlockMechanic
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

class NexoNoteBlockPlaceEvent(
    override val mechanic: NoteBlockMechanic,
    block: Block,
    player: Player,
    itemInHand: ItemStack,
    hand: EquipmentSlot
) : NexoBlockPlaceEvent(mechanic, block, player, itemInHand, hand), Cancellable {

    override fun getHandlers() = handlerList

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
