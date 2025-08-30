package com.nexomc.nexo.mechanics.furniture.evolution

import com.jeff_media.morepersistentdatatypes.DataType
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.utils.SchedulerUtils
import com.nexomc.nexo.utils.to
import kotlinx.coroutines.Job
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.ItemDisplay
import org.bukkit.persistence.PersistentDataType
import kotlin.random.Random

object EvolutionJob {

    fun launchJob(delay: Int): Job {
        return SchedulerUtils.launchRepeating(0L, delay.toLong()) {
            SchedulerUtils.runAtWorldEntities<ItemDisplay> { entity ->
                val (entityLoc, world, pdc) = entity.location to entity.world to entity.persistentDataContainer
                if (!pdc.has(FurnitureMechanic.EVOLUTION_KEY, PersistentDataType.INTEGER)) return@runAtWorldEntities

                val mechanic = NexoFurniture.furnitureMechanic(entity) ?: return@runAtWorldEntities
                if (mechanic.farmlandRequired && entityLoc.block.getRelative(BlockFace.DOWN).type != Material.FARMLAND) {
                    NexoFurniture.remove(entity, null)
                    return@runAtWorldEntities
                }

                val evolution = mechanic.evolution ?: return@runAtWorldEntities
                val lightBoostTick = evolution.lightBoostTick.takeIf { entityLoc.block.lightLevel >= evolution.minimumLightLevel } ?: 0
                val rainBoostTick = evolution.rainBoostTick.takeIf { world.hasStorm() && world.getHighestBlockAt(entityLoc).y > entityLoc.y } ?: 0
                val evolutionStep = (pdc.get(FurnitureMechanic.EVOLUTION_KEY, DataType.INTEGER)?.plus(delay) ?: 1) + lightBoostTick + rainBoostTick

                if (evolutionStep < evolution.delay) pdc.set(FurnitureMechanic.EVOLUTION_KEY, DataType.INTEGER, evolutionStep)
                else {
                    if (evolution.nextStage == null || Random.nextInt(evolution.probability) != 0) return@runAtWorldEntities
                    val nextMechanic = NexoFurniture.furnitureMechanic(evolution.nextStage) ?: return@runAtWorldEntities
                    NexoFurniture.remove(entity)
                    nextMechanic.place(entity.location)
                }
            }
        }
    }
}
