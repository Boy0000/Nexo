package com.nexomc.nexo.api.events.furniture

import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

class NexoFurnitureInteractEvent @JvmOverloads constructor(
    val mechanic: FurnitureMechanic,
    val baseEntity: ItemDisplay,
    val player: Player,
    itemInHand: ItemStack?,
    val hand: EquipmentSlot,
    val interactionPoint: Location? = baseEntity.location,
    var useFurniture: Result = Result.DEFAULT,
    var useItemInHand: Result = Result.DEFAULT,
    /**
     * Returns the clicked blockface for this interaction.
     * Only applies if the interaction was with a barrier-hitbox block.
     * If interaction-hitbox, this will be null.
     */
    val blockFace: BlockFace? = null,
) : Event(), Cancellable {
    val itemInHand: ItemStack = itemInHand ?: ItemStack(Material.AIR)
    private var isCancelled = false

    override fun isCancelled(): Boolean {
        return isCancelled || useFurniture == Result.DENY
    }

    override fun setCancelled(cancel: Boolean) {
        useFurniture = when {
            cancel -> Result.DENY
            useFurniture == Result.DENY -> Result.DEFAULT
            else -> useFurniture
        }
        useItemInHand = when {
            cancel -> Result.DENY
            useItemInHand == Result.DENY -> Result.DEFAULT
            else -> useItemInHand
        }
        isCancelled = cancel
    }

    override fun getHandlers() = handlerList

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
