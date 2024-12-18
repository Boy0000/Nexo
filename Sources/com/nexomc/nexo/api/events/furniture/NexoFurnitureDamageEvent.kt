package com.nexomc.nexo.api.events.furniture

import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import org.bukkit.block.Block
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Event fired right before a player damages the Furniture.
 * If cancelled, the block will not be damaged.
 * @see FurnitureMechanic
 */
class NexoFurnitureDamageEvent
/**
 * @param mechanic The FurnitureMechanic of this block
 * @param baseEntity The base-entity for the damaged furniture
 * @param player The player who damaged this block
 * @param block The block that was damaged
 */ @JvmOverloads constructor(
    /**
     * @return The FurnitureMechanic of this Furniture
     */
    val mechanic: FurnitureMechanic, val baseEntity: ItemDisplay,
    /**
     * @return The player that damaged the furniture
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
