package com.nexomc.nexo.converter

import com.jeff_media.customblockdata.CustomBlockData
import com.mineinabyss.idofront.items.asColorable
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.mechanics.furniture.FurnitureHelpers
import com.nexomc.nexo.utils.associateFastWith
import com.nexomc.nexo.utils.filterFast
import com.nexomc.nexo.utils.filterFastIsInstance
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.mapFast
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.EntitiesLoadEvent
import org.bukkit.persistence.PersistentDataType

@Suppress("DEPRECATION")
class OraxenConverterListener : Listener {

    private val FURNITURE_KEY: NamespacedKey = NamespacedKey.fromString("oraxen:furniture")!!
    private val BASE_ENTITY_KEY: NamespacedKey = NamespacedKey.fromString("oraxen:base_entity")!!
    private val SEAT_KEY: NamespacedKey = NamespacedKey.fromString("oraxen:seat")!!

    @EventHandler
    fun ChunkLoadEvent.onChunkLoad() {
        CustomBlockData.getBlocksWithCustomData("oraxen", chunk)
            .associateFastWith { CustomBlockData(it, "oraxen") }
            .filterFast { it.key.type == Material.BARRIER && BASE_ENTITY_KEY in it.value.keys }
            .forEach { (block, pdc) ->
                block.type = Material.AIR
                pdc.clear()
            }
    }

    @EventHandler
    fun EntitiesLoadEvent.onEntitiesLoad() {
        entities.filterFastIsInstance<Interaction> { it.persistentDataContainer.let { i -> i.has(BASE_ENTITY_KEY) && !i.has(SEAT_KEY) } }.forEach(Interaction::remove)
        entities.filterFastIsInstance<ArmorStand> { it.persistentDataContainer.has(FURNITURE_KEY) }.forEach(ArmorStand::remove)
        entities.filterFastIsInstance<ItemFrame> { it.persistentDataContainer.has(FURNITURE_KEY) }.forEach {
            Logs.logError("Found legacy Oraxen-Furniture ${it.persistentDataContainer.get(FURNITURE_KEY, PersistentDataType.STRING)} using ItemFrame at ${it.location.fineString()}...")
            Logs.logWarn("Nexo only supports ItemDisplay-Furniture, we suggest manually replacing these")
        }
        entities.mapFast(Entity::getPersistentDataContainer).forEach(OraxenConverter::convertOraxenPDCEntries)
        entities.filterFastIsInstance<ItemDisplay>(NexoFurniture::isFurniture).forEach { baseEntity ->
            val color = FurnitureHelpers.furnitureDye(baseEntity) ?: baseEntity.itemStack.itemMeta?.asColorable()?.color ?: return@forEach
            FurnitureHelpers.furnitureDye(baseEntity, color)
        }
    }
}