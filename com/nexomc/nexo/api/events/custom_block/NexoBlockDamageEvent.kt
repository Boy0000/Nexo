package com.nexomc.nexo.api.events.custom_block

import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteBlockMechanic
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Event fired right before a player damages a NoteBlock.
 * If cancelled, the block will not be damaged.
 * @see NoteBlockMechanic
 */
open class NexoBlockDamageEvent
/**
 * @param mechanic The CustomBlockMechanic of this block
 * @param block The block that was damaged
 * @param player The player who damaged this block
 */(
    /**
     * @return The CustomBlockMechanic
     */
    open val mechanic: CustomBlockMechanic,
    /**
     * @return The block that was broken
     */
    val block: Block,
    /**
     * @return The player who broke the note block
     */
    val player: Player
) : Event(), Cancellable {
    private var isCancelled = false

    override fun isCancelled(): Boolean {
        return isCancelled
    }

    override fun setCancelled(cancel: Boolean) {
        isCancelled = cancel
    }

    override fun getHandlers() = handlerList

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
