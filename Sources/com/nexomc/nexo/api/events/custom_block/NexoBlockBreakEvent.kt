package com.nexomc.nexo.api.events.custom_block

import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteBlockMechanic
import com.nexomc.nexo.utils.drops.Drop
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

open class NexoBlockBreakEvent(
    /**
     * @return The CustomBlockMechanic of this block
     */
    open val mechanic: CustomBlockMechanic,
    /**
     * @return The block that was broken
     */
    val block: Block,
    /**
     * @return The player who broke this block
     */
    val player: Player
) : Event(), Cancellable {
    constructor(mechanic: NoteBlockMechanic, block: Block, player: Player, drop: Drop) : this(mechanic, block, player) {
        this.drop = drop
    }

    /**
     * @return The drop of the block
     */
    /**
     * Set the drop of the block
     * @param drop the new drop
     */
    lateinit var drop: Drop

    private var isCancelled = false

    override fun isCancelled(): Boolean {
        return isCancelled
    }

    override fun setCancelled(cancel: Boolean) {
        isCancelled = cancel
    }

    override fun getHandlers() = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}
