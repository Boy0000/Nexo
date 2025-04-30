package com.nexomc.nexo.mechanics.custom_block.stringblock.sapling

import com.jeff_media.customblockdata.CustomBlockData
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.compatibilities.worldedit.WrappedWorldEdit
import com.nexomc.nexo.utils.BlockHelpers.persistentDataContainer
import com.nexomc.nexo.utils.SchedulerUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable

class SaplingTask(private val delay: Int) : BukkitRunnable() {

    override fun run() {
        Bukkit.getWorlds().forEach { world ->
            world.loadedChunks.forEach { chunk ->
                CustomBlockData.getBlocksWithCustomData(NexoPlugin.instance(), chunk).forEach { block ->
                    SchedulerUtils.foliaScheduler.runAtLocationTimer(block.location, Runnable {
                        val pdc = block.persistentDataContainer
                        when {
                            pdc.has(SaplingMechanic.SAPLING_KEY, PersistentDataType.INTEGER) && block.type == Material.TRIPWIRE -> {
                                val sapling = NexoBlocks.stringMechanic(block)?.sapling() ?: return@Runnable
                                if (!sapling.hasSchematic() || !sapling.canGrowNaturally) return@Runnable
                                if (sapling.requiresWaterSource && !sapling.isUnderWater(block)) return@Runnable
                                if (sapling.requiresLight() && block.lightLevel < sapling.minLightLevel) return@Runnable

                                val selectedSchematic = sapling.selectSchematic()
                                if (selectedSchematic == null || (!sapling.replaceBlocks && WrappedWorldEdit.blocksInSchematic(block.location, selectedSchematic).isNotEmpty())) return@Runnable

                                val growthTimeRemains = pdc.getOrDefault(SaplingMechanic.SAPLING_KEY, PersistentDataType.INTEGER, 0) - delay
                                if (growthTimeRemains <= 0) {
                                    block.setType(Material.AIR, false)
                                    if (sapling.hasGrowSound()) block.world.playSound(block.location, sapling.growSound!!, 1.0f, 0.8f)
                                    WrappedWorldEdit.pasteSchematic(block.location, selectedSchematic, sapling.replaceBlocks, sapling.copyBiomes, sapling.copyEntities)
                                } else pdc.set(SaplingMechanic.SAPLING_KEY, PersistentDataType.INTEGER, growthTimeRemains)
                            }
                            pdc.has(SaplingMechanic.SAPLING_KEY, PersistentDataType.INTEGER) && block.type != Material.TRIPWIRE -> pdc.remove(SaplingMechanic.SAPLING_KEY)
                        }
                    }, 1, delay.toLong())
                }
            }
        }
    }
}
