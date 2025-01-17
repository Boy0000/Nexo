package com.nexomc.nexo.api.events.custom_block.chorusblock

import com.nexomc.nexo.api.events.custom_block.NexoBlockBreakEvent
import com.nexomc.nexo.mechanics.custom_block.chorusblock.ChorusBlockMechanic
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteBlockMechanic
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList

/**
 * @param mechanic The CustomBlockMechanic of this block
 * @param block    The block that was damaged
 * @param player   The player who damaged this block
 */
class NexoChorusBlockBreakEvent(
    override val mechanic: ChorusBlockMechanic,
    block: Block,
    player: Player,
) : NexoBlockBreakEvent(mechanic, block, player), Cancellable {

    init {
        drop = mechanic.breakable.drop
    }

    override fun getHandlers() = handlerList

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
