package com.nexomc.nexo.converter

import com.jeff_media.customblockdata.CustomBlockData
import com.mineinabyss.idofront.items.asColorable
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.mechanics.furniture.FurnitureHelpers
import com.nexomc.nexo.nms.NMSHandlers
import com.nexomc.nexo.utils.SchedulerUtils
import com.nexomc.nexo.utils.associateFastWith
import com.nexomc.nexo.utils.filterFast
import com.nexomc.nexo.utils.logs.Logs
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.ItemFrame
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

    init {
        // Handles already loaded spawn-chunks
        SchedulerUtils.runAtWorldEntities { entity ->
            entity.convertFurniture()
        }
    }

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
        entities.forEach { it.convertFurniture() }
    }

    private fun Entity.convertFurniture() {
        val pdc = persistentDataContainer
        when (val baseEntity = this) {
            is Interaction -> baseEntity.takeIf { i -> pdc.has(BASE_ENTITY_KEY) && !pdc.has(SEAT_KEY) }?.remove()
            is ArmorStand -> baseEntity.takeIf { pdc.has(FURNITURE_KEY) }?.remove()
            is ItemFrame -> baseEntity.takeIf { pdc.has(FURNITURE_KEY) }?.also {
                Logs.logError("Found legacy Oraxen-Furniture ${pdc.get(FURNITURE_KEY, PersistentDataType.STRING)} using ItemFrame at ${it.location.fineString()}...")
                Logs.logWarn("Nexo only supports ItemDisplay-Furniture, we suggest manually replacing these")
            }
            is ItemDisplay -> when {
                !NexoFurniture.isFurniture(baseEntity) -> NMSHandlers.handler().pluginConverter.convertOraxen(baseEntity)
                else -> {
                    val color = FurnitureHelpers.furnitureDye(baseEntity) ?: baseEntity.itemStack.itemMeta?.asColorable()?.color ?: return
                    FurnitureHelpers.furnitureDye(baseEntity, color)
                }
            }
        }
    }
}