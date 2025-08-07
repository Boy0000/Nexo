package com.nexomc.nexo.mechanics.furniture.compatibility

import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.api.events.furniture.NexoFurnitureBreakEvent
import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent
import com.nexomc.nexo.utils.EventUtils.call
import com.nexomc.protectionlib.ProtectionLib
import com.ticxo.modelengine.api.events.BaseEntityInteractEvent
import org.bukkit.GameMode
import org.bukkit.entity.ItemDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.EquipmentSlot

class ModelEngineCompatibility : Listener {

    @EventHandler
    fun BaseEntityInteractEvent.onMegFurnitureInteract() {
        val itemStack = when (slot) {
            EquipmentSlot.HAND -> player.inventory.itemInMainHand
            else -> player.inventory.itemInOffHand
        }
        val interactionPoint = clickedPosition?.toLocation(player.world)
        val baseEntity = baseEntity.original as? ItemDisplay ?: return
        val mechanic = NexoFurniture.furnitureMechanic(baseEntity) ?: return

        when {
            action == BaseEntityInteractEvent.Action.ATTACK && player.gameMode != GameMode.ADVENTURE && ProtectionLib.canBreak(player, baseEntity.location) -> {
                BlockBreakEvent(baseEntity.location.block, player).call {
                    NexoFurnitureBreakEvent(mechanic, baseEntity, player).call {
                        NexoFurniture.remove(baseEntity, player)
                    }
                }
            }

            action != BaseEntityInteractEvent.Action.ATTACK && ProtectionLib.canInteract(player, baseEntity.location) && clickedPosition != null ->
                NexoFurnitureInteractEvent(mechanic, baseEntity, player, itemStack, slot, interactionPoint).call()
        }
    }
}