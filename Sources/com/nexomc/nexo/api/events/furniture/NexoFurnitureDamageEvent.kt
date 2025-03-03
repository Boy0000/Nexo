package com.nexomc.nexo.api.events.furniture

import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Event fired right before a player damages the Furniture.
 * If cancelled, the block will not be damaged.
 * @see FurnitureMechanic
 * @param mechanic The FurnitureMechanic of this block
 * @param baseEntity The base-entity for the damaged furniture
 * @param player The player who damaged this block
 */
class NexoFurnitureDamageEvent(
    val mechanic: FurnitureMechanic,
    val baseEntity: ItemDisplay,
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
