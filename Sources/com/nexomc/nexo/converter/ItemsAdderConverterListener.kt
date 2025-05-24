package com.nexomc.nexo.converter

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.mechanics.furniture.FurnitureHelpers
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.mechanics.furniture.seats.FurnitureSeat
import com.nexomc.nexo.utils.SchedulerUtils
import com.nexomc.nexo.utils.asColorable
import com.nexomc.nexo.utils.serialize
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.ItemDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.EntitiesLoadEvent
import org.bukkit.persistence.PersistentDataType

class ItemsAdderConverterListener : Listener {

    init {
        // Handles already loaded spawn-chunks
        SchedulerUtils.runAtWorldEntities<ItemDisplay>(::convertFurniture)
    }

    @EventHandler
    fun EntitiesLoadEvent.onLoadFurniture() {
        entities.forEach(::convertFurniture)
    }

    companion object {
        val PLACEABLE_BEHAVIOUR_KEY: NamespacedKey = NamespacedKey.fromString("itemsadder:placeable_behaviour_type")!!
        val PLACEABLE_ITEM_KEY = NamespacedKey.fromString("itemsadder:placeable_entity_item")!!

        fun convertFurniture(entity: Entity) {
            val baseEntity = entity as? ItemDisplay ?: return
            val pdc = baseEntity.persistentDataContainer
            val (namespace, id) = pdc.get(PLACEABLE_ITEM_KEY, PersistentDataType.STRING)?.split(":") ?: return
            val itemId = NexoPlugin.instance().converter().itemsadderConverter.changedItemIds["$namespace:$id"] ?: id
            val mechanic = NexoFurniture.furnitureMechanic(itemId) ?: return
            if (pdc.get(PLACEABLE_BEHAVIOUR_KEY, PersistentDataType.STRING) != "furniture") return

            pdc.remove(PLACEABLE_BEHAVIOUR_KEY)
            pdc.remove(PLACEABLE_ITEM_KEY)
            pdc.set(FurnitureMechanic.FURNITURE_KEY, PersistentDataType.STRING, itemId)

            FurnitureSeat.spawnSeats(baseEntity, mechanic)
            FurnitureHelpers.furnitureDye(baseEntity, baseEntity.itemStack.asColorable()?.color)
            baseEntity.itemStack.itemMeta?.displayName()?.serialize()?.also {
                pdc.set(FurnitureMechanic.DISPLAY_NAME_KEY, PersistentDataType.STRING, it)
            }
            baseEntity.setItemStack(null)
            //baseEntity.transformation = baseEntity.transformation.apply {
            //    scale.set(mechanic.properties.scale)
            //    translation.set(0.0, 0.0, 0.0)
            //    rightRotation.set(0f, 0f, 0f, 1f)
            //}
            //baseEntity.itemDisplayTransform = mechanic.properties.displayTransform
            //if (baseEntity.itemDisplayTransform == FurnitureTransform.FIXED) baseEntity.teleportAsync(baseEntity.location.apply { y += 0.5 })
            //if (baseEntity.itemDisplayTransform == FurnitureTransform.FIXED) setRotation(baseEntity.yaw, -90f)
            NexoFurniture.updateFurniture(baseEntity)
        }
    }
}