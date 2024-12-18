package com.nexomc.nexo.converter

import com.nexomc.nexo.items.ItemUpdater
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.utils.logs.Logs
import com.jeff_media.customblockdata.CustomBlockData
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemFrame
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.EntitiesLoadEvent
import org.bukkit.persistence.PersistentDataType

class ConverterListener : Listener {

    private val FURNITURE_KEY: NamespacedKey = NamespacedKey.fromString("oraxen:furniture")!!
    private val BASE_ENTITY_KEY: NamespacedKey = NamespacedKey.fromString("oraxen:base_entity")!!
    private val SEAT_KEY: NamespacedKey = NamespacedKey.fromString("oraxen:seat")!!

    @EventHandler
    fun ChunkLoadEvent.onChunkLoad() {
        CustomBlockData.getBlocksWithCustomData("oraxen", chunk)
            .associateWith { CustomBlockData(it, "oraxen") }
            .filter { it.key.type == Material.BARRIER && BASE_ENTITY_KEY in it.value.keys }
            .forEach { (block, pdc) ->
                block.type = Material.AIR
                pdc.clear()
            }
    }

    @EventHandler
    fun EntitiesLoadEvent.onEntitiesLoad() {
        entities.filterIsInstance<Interaction>().filter { it.persistentDataContainer.let { i -> i.has(BASE_ENTITY_KEY) && !i.has(SEAT_KEY) } }.forEach(Interaction::remove)
        entities.filterIsInstance<ArmorStand>().filter { it.persistentDataContainer.has(FURNITURE_KEY) }.forEach(ArmorStand::remove)
        entities.filterIsInstance<ItemFrame>().filter { it.persistentDataContainer.has(FURNITURE_KEY) }.forEach {
            Logs.logError("Found legacy Oraxen-Furniture ${it.persistentDataContainer.get(FURNITURE_KEY, PersistentDataType.STRING)} using ItemFrame at ${it.location.fineString()}...")
            Logs.logWarn("Nexo only supports ItemDisplay-Furniture, we suggest manually replacing these")
        }
        entities.map(Entity::getPersistentDataContainer).forEach(OraxenConverter::convertOraxenPDCEntries)
    }
}