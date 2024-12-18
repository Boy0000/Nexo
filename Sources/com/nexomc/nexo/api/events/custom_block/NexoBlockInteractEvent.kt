package com.nexomc.nexo.api.events.custom_block

import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.event.block.Action
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

open class NexoBlockInteractEvent(
    /**
     * @return The NoteBlockMechanic of this block
     */
    open val mechanic: CustomBlockMechanic,
    /**
     * @return The player who interacted with this block
     */
    val player: Player,
    /**
     * @return The item in hand when the player interacted with the note block
     */
    val itemInHand: ItemStack?,
    /**
     * @return The hand used to perform interaction
     */
    val hand: EquipmentSlot,
    /**
     * @return The block that was interacted with
     */
    val block: Block,
    /**
     * @return The BlockFace that was clicked
     */
    val blockFace: BlockFace,
    /**
     * @return The type of interaction
     */
    val action: Action
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
