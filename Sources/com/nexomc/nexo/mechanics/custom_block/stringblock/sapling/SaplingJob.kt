package com.nexomc.nexo.mechanics.custom_block.stringblock.sapling

import com.jeff_media.customblockdata.CustomBlockData
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.utils.BlockHelpers.persistentDataContainer
import com.nexomc.nexo.utils.SchedulerUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.persistence.PersistentDataType

object SaplingJob {

    fun launchJob(delay: Int): Job {
        return SchedulerUtils.launchRepeating(0L, delay.toLong()) {
            for (world in Bukkit.getWorlds()) world.loadedChunks.forEach { chunk ->
                CustomBlockData.getBlocksWithCustomData(NexoPlugin.instance(), chunk).forEach { block ->
                    val location = block.location
                    withContext(SchedulerUtils.regionDispatcher(location)) {
                        val pdc = block.persistentDataContainer
                        when {
                            pdc.has(SaplingMechanic.SAPLING_KEY, PersistentDataType.INTEGER) && block.type == Material.TRIPWIRE -> {
                                val sapling = NexoBlocks.stringMechanic(block)?.sapling() ?: return@withContext
                                if (!sapling.hasSchematic() || !sapling.canGrowNaturally) return@withContext
                                if (sapling.requiresWaterSource && !sapling.isUnderWater(block)) return@withContext
                                if (sapling.requiresLight() && block.lightLevel < sapling.minLightLevel) return@withContext

                                val selectedSchematic = sapling.selectSchematic() ?: return@withContext
                                if (!sapling.canPlaceSchematic(location, selectedSchematic)) return@withContext

                                val growthTimeRemains = pdc.getOrDefault(SaplingMechanic.SAPLING_KEY, PersistentDataType.INTEGER, 0) - delay
                                if (growthTimeRemains <= 0) {
                                    sapling.placeSchematic(location, selectedSchematic)
                                } else pdc.set(SaplingMechanic.SAPLING_KEY, PersistentDataType.INTEGER, growthTimeRemains)
                            }
                            pdc.has(SaplingMechanic.SAPLING_KEY, PersistentDataType.INTEGER) && block.type != Material.TRIPWIRE -> pdc.remove(SaplingMechanic.SAPLING_KEY)
                        }
                    }
                }
            }
        }
    }
}
