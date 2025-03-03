package com.nexomc.nexo.api.events.furniture

import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.utils.drops.Drop
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class NexoFurnitureBreakEvent(
    /**
     * @return The FurnitureMechanic of this Furniture
     */
    val mechanic: FurnitureMechanic, val baseEntity: ItemDisplay,
    /**
     * @return The player that broke the furniture
     */
    val player: Player
) : Event(), Cancellable {
    private var isCancelled = false
    /**
     * Set the drop of the furniture
     * @param drop the new drop
     * @return The drop of the furniture
     */
    var drop: Drop = mechanic.breakable.drop


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
