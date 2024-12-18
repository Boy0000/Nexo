package com.nexomc.nexo.api.events.furniture

import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import org.bukkit.block.Block
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

class NexoFurniturePlaceEvent(
    /**
     * @return The FurnitureMechanic of this furniture
     */
    val mechanic: FurnitureMechanic,
    /**
     * @return The block this furniture was placed at
     */
    val block: Block,
    /**
     * @return The item frame for this furniture
     */
    val baseEntity: ItemDisplay,
    /**
     * @return The player who placed this furniture
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
     * @return The hand used to place the furniture
     */
    val hand: EquipmentSlot
) : Event(), Cancellable {
    private var isCancelled = false

    override fun isCancelled(): Boolean {
        return isCancelled
    }

    override fun setCancelled(cancelled: Boolean) {
        isCancelled = cancelled
    }

    override fun getHandlers() = handlerList

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
