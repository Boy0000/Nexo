package com.nexomc.nexo.mechanics.furniture.evolution

import com.jeff_media.morepersistentdatatypes.DataType
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent
import com.nexomc.nexo.mechanics.furniture.FurnitureFactory
import com.nexomc.nexo.mechanics.furniture.FurnitureHelpers
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import org.bukkit.Effect
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.inventory.EquipmentSlot
import kotlin.random.Random

class EvolutionListener : Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun NexoFurnitureInteractEvent.onBoneMeal() {
        if (hand != EquipmentSlot.HAND || !mechanic.hasEvolution) return
        if (!baseEntity.persistentDataContainer.has(FurnitureMechanic.EVOLUTION_KEY, DataType.INTEGER)) return
        if (itemInHand.type != Material.BONE_MEAL) return

        isCancelled = true
        val evolution = mechanic.evolution?.takeUnless { it.nextStage == null || it.boneMealChance <= 0 } ?: return
        val nextMechanic = FurnitureFactory.instance()?.getMechanic(evolution.nextStage) ?: return
        val nextItem = nextMechanic.placedItem(baseEntity).build()

        itemInHand.amount -= 1
        baseEntity.world.playEffect(baseEntity.location, Effect.BONE_MEAL_USE, 3)
        if (Random.nextDouble() > evolution.boneMealChance.toDouble()) return
        FurnitureHelpers.furnitureItem(baseEntity, nextItem)
        baseEntity.persistentDataContainer.set(FurnitureMechanic.FURNITURE_KEY, DataType.STRING, "nexo:${nextMechanic.itemID}")
    }
}
