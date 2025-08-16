package com.nexomc.nexo.api.events.custom_block

import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

open class NexoBlockPlaceEvent(
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
    val player: Player,
    /**
     * Gets the item in the player's hand when they placed the furniture.
     *
     * @return The ItemStack for the item in the player's hand when they
     * placed the furniture
     */
    val itemInHand: ItemStack,
    /**
     * Gets the hand used to place the furniture.
     *
     * @return The EquipmentSlot for the hand used to place the furniture
     */
    val hand: EquipmentSlot
) : Event(), Cancellable {
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
