package com.nexomc.nexo.api.events.custom_block.noteblock

import com.nexomc.nexo.api.events.custom_block.NexoBlockDamageEvent
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteBlockMechanic
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList

/**
 * Event fired right before a player damages a NoteBlock.
 * If cancelled, the block will not be damaged.
 * @see NoteBlockMechanic
 * @param mechanic The CustomBlockMechanic of this block
 * @param block    The block that was damaged
 * @param player   The player who damaged this block
 */
class NexoNoteBlockDamageEvent(
    override val mechanic: NoteBlockMechanic,
    block: Block,
    player: Player
) : NexoBlockDamageEvent(mechanic, block, player), Cancellable {

    override fun getHandlers() = handlerList

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
