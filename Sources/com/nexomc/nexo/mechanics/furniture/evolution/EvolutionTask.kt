package com.nexomc.nexo.mechanics.furniture.evolution

import com.nexomc.nexo.utils.to
import com.jeff_media.morepersistentdatatypes.DataType
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.mechanics.furniture.FurnitureFactory
import com.nexomc.nexo.mechanics.furniture.FurnitureHelpers
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.utils.SchedulerUtils
import com.nexomc.nexo.utils.filterFastIsInstance
import com.nexomc.nexo.utils.flatMapFast
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.entity.ItemDisplay
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import kotlin.random.Random

class EvolutionTask(private val furnitureFactory: FurnitureFactory, private val delay: Int) : BukkitRunnable() {
    override fun run() {
        Bukkit.getWorlds().forEach { world ->
            world.loadedChunks.forEach { chunk ->
                SchedulerUtils.foliaScheduler.runAtLocation(Location(world, chunk.x * 16.0, 100.0, chunk.z * 16.0)) {
                    chunk.entities.forEach { entity ->
                        SchedulerUtils.foliaScheduler.runAtEntity(entity) {
                            val (entityLoc, world, pdc) = entity.location to entity.world to entity.persistentDataContainer
                            if (!pdc.has(FurnitureMechanic.EVOLUTION_KEY, PersistentDataType.INTEGER)) return@runAtEntity

                            val mechanic = NexoFurniture.furnitureMechanic(entity) ?: return@runAtEntity
                            if (mechanic.farmlandRequired && entityLoc.block.getRelative(BlockFace.DOWN).type != Material.FARMLAND) {
                                NexoFurniture.remove(entity, null)
                                return@runAtEntity
                            }

                            val evolution = mechanic.evolution ?: return@runAtEntity
                            val lightBoostTick = evolution.lightBoostTick.takeIf { entityLoc.block.lightLevel >= evolution.minimumLightLevel } ?: 0
                            val rainBoostTick = evolution.rainBoostTick.takeIf { world.hasStorm() && world.getHighestBlockAt(entityLoc).y > entityLoc.y } ?: 0
                            val evolutionStep = (pdc.get(FurnitureMechanic.EVOLUTION_KEY, DataType.INTEGER)?.plus(delay) ?: 1) + lightBoostTick + rainBoostTick

                            if (evolutionStep < evolution.delay) pdc.set(FurnitureMechanic.EVOLUTION_KEY, DataType.INTEGER, evolutionStep)
                            else {
                                if (evolution.nextStage == null || Random.nextInt(evolution.probability) != 0) return@runAtEntity
                                val nextMechanic = furnitureFactory.getMechanic(evolution.nextStage) ?: return@runAtEntity
                                NexoFurniture.remove(entity)
                                nextMechanic.place(entity.location)
                            }
                        }
                    }
                }
            }
        }
    }
}
