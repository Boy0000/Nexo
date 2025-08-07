package com.nexomc.nexo.mechanics.custom_block.noteblock.beacon

import com.destroystokyo.paper.event.block.BeaconEffectEvent
import com.nexomc.nexo.api.NexoBlocks
import org.bukkit.Location
import org.bukkit.Tag
import org.bukkit.block.Beacon
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class BeaconListener : Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun BeaconEffectEvent.beaconEffect() {
        if (!checkBeaconPyramid(block)) isCancelled = true
    }

    companion object {
        private fun checkBeaconPyramid(block: Block): Boolean {
            val beacon = block.state as? Beacon ?: return false
            val (x, y) = block.x to block.y
            val (z, world) = block.z to block.world

            var validPyramid = true
            for (tier in 1..beacon.tier) {
                val tierY = y - tier

                if (tierY < world.minHeight) break


                var validTier = true
                for (tierX in x - tier..x + tier) {
                    for (tierZ in z - tier..z + tier) {
                        val tierBlockLoc =
                            Location(world, tierX.toDouble(), tierY.toDouble(), tierZ.toDouble())
                        val blockData = world.getBlockData(tierBlockLoc)
                        if (!Tag.BEACON_BASE_BLOCKS.isTagged(blockData.material)) {
                            validTier = false
                            break
                        }

                        val mechanic = NexoBlocks.noteBlockMechanic(blockData)
                        if (mechanic != null && !mechanic.isBeaconBaseBlock()) {
                            validTier = false
                            break
                        }
                    }
                }

                if (!validTier) {
                    validPyramid = false
                    break
                }
            }

            return validPyramid
        }
    }
}
