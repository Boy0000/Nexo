package com.nexomc.nexo.mechanics.furniture.evolution

import com.nexomc.nexo.utils.to
import com.jeff_media.morepersistentdatatypes.DataType
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.mechanics.furniture.FurnitureFactory
import com.nexomc.nexo.mechanics.furniture.FurnitureHelpers
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.entity.ItemDisplay
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import kotlin.random.Random

class EvolutionTask(private val furnitureFactory: FurnitureFactory, private val delay: Int) : BukkitRunnable() {
    override fun run() {
        Bukkit.getWorlds().flatMap { it.entities }.filterIsInstance<ItemDisplay>().forEach { entity ->
            val (entityLoc, world, pdc) = entity.location to entity.world to entity.persistentDataContainer
            if (!pdc.has(FurnitureMechanic.EVOLUTION_KEY, PersistentDataType.INTEGER)) return@forEach

            val blockBelow = entityLoc.block.getRelative(BlockFace.DOWN)
            val mechanic = NexoFurniture.furnitureMechanic(entity) ?: return@forEach

            if (mechanic.farmlandRequired && blockBelow.type != Material.FARMLAND) {
                NexoFurniture.remove(entity, null)
                return@forEach
            }

            val evolution = mechanic.evolution ?: return@forEach
            val lightBoostTick = evolution.lightBoostTick.takeIf { entityLoc.block.lightLevel >= evolution.minimumLightLevel } ?: 0
            val rainBoostTick = evolution.rainBoostTick.takeIf { world.hasStorm() && world.getHighestBlockAt(entityLoc).y > entityLoc.y } ?: 0
            val evolutionStep = (pdc.get(FurnitureMechanic.EVOLUTION_KEY, DataType.INTEGER)?.plus(delay) ?: 1) + lightBoostTick + rainBoostTick

            if (evolutionStep < evolution.delay) pdc.set(FurnitureMechanic.EVOLUTION_KEY, DataType.INTEGER, evolutionStep)
            else {
                if (evolution.nextStage == null || !evolution.bernoulliTest()) return@forEach
                val nextMechanic = furnitureFactory.getMechanic(evolution.nextStage) ?: return@forEach
                NexoFurniture.remove(entity)
                nextMechanic.place(entity.location)
            }
        }
    }
}
