package com.nexomc.nexo.converter

import com.jeff_media.customblockdata.CustomBlockData
import com.mineinabyss.idofront.items.asColorable
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.mechanics.furniture.FurnitureHelpers
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.mechanics.furniture.seats.FurnitureSeat
import com.nexomc.nexo.utils.*
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.ItemDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.EntitiesLoadEvent
import org.bukkit.persistence.PersistentDataType

class ItemsAdderConverterListener : Listener {

    private val PLACEABLE_BEHAVIOUR_KEY: NamespacedKey = NamespacedKey.fromString("itemsadder:placeable_behaviour_type")!!
    private val PLACEABLE_ITEM_KEY = NamespacedKey.fromString("itemsadder:placeable_entity_item")!!
    private val BASE_ENTITY_KEY: NamespacedKey = NamespacedKey.fromString("oraxen:base_entity")!!
    private val SEAT_KEY: NamespacedKey = NamespacedKey.fromString("oraxen:seat")!!

    @EventHandler
    fun EntitiesLoadEvent.onLoadFurniture() {
        entities.filterFastIsInstance<ItemDisplay> { !NexoFurniture.isFurniture(it) }.forEach { baseEntity ->
            val pdc = baseEntity.persistentDataContainer
            val itemId = pdc.get(PLACEABLE_ITEM_KEY, PersistentDataType.STRING)?.substringAfter(":") ?: return@forEach
            val mechanic = NexoFurniture.furnitureMechanic(itemId) ?: return@forEach
            if (pdc.get(PLACEABLE_BEHAVIOUR_KEY, PersistentDataType.STRING) != "furniture") return@forEach

            pdc.remove(PLACEABLE_BEHAVIOUR_KEY)
            pdc.remove(PLACEABLE_ITEM_KEY)
            pdc.set(FurnitureMechanic.FURNITURE_KEY, PersistentDataType.STRING, itemId)

            FurnitureSeat.spawnSeats(baseEntity, mechanic)
            FurnitureHelpers.furnitureDye(baseEntity, baseEntity.itemStack.itemMeta?.asColorable()?.color)
            baseEntity.itemStack.itemMeta?.displayName()?.serialize()?.also {
                pdc.set(FurnitureMechanic.DISPLAY_NAME_KEY, PersistentDataType.STRING, it)
            }
            baseEntity.setItemStack(null)
        }
    }
}