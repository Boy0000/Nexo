package com.nexomc.nexo.mechanics.furniture.door

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.api.events.NexoItemsLoadedEvent
import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager.Companion.furnitureBaseMap
import com.nexomc.nexo.utils.SchedulerUtils
import io.papermc.paper.event.player.PlayerTrackEntityEvent
import org.bukkit.entity.Entity
import org.bukkit.entity.ItemDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.EquipmentSlot

class FurnitureDoorListener : Listener {

    @EventHandler
    fun PlayerTrackEntityEvent.onTrackDoor() {
        val baseEntity = entity.takeIf(Entity::isValid) as? ItemDisplay ?: return
        val mechanic = NexoFurniture.furnitureMechanic(baseEntity) ?: return

        SchedulerUtils.foliaScheduler.runAtEntityLater(baseEntity, Runnable {
            mechanic.door?.ensureHitboxState(baseEntity, mechanic, player)
        }, 6L)
    }

    @EventHandler
    fun EntityAddToWorldEvent.onLoad() {
        val baseEntity = entity as? ItemDisplay ?: return
        furnitureBaseMap.remove(baseEntity.uniqueId, furnitureBaseMap.get(baseEntity.uniqueId)?.takeIf { it.baseId != baseEntity.entityId })

        SchedulerUtils.foliaScheduler.runAtEntityLater(baseEntity, Runnable {
            val mechanic = NexoFurniture.furnitureMechanic(baseEntity) ?: return@Runnable
            mechanic.door?.ensureHitboxState(baseEntity, mechanic)
        }, 4L)
    }

    @EventHandler
    fun NexoItemsLoadedEvent.onFurnitureFactory() {
        SchedulerUtils.runTaskLater(2L) {
            SchedulerUtils.runAtWorldEntities<ItemDisplay> { baseEntity ->
                val mechanic = NexoFurniture.furnitureMechanic(baseEntity) ?: return@runAtWorldEntities
                mechanic.door?.ensureHitboxState(baseEntity, mechanic)
            }
        }
    }

    @EventHandler
    fun NexoFurnitureInteractEvent.onDoorOpen() {
        if (hand == EquipmentSlot.HAND) mechanic.door?.toggleState(baseEntity, mechanic)
    }
}