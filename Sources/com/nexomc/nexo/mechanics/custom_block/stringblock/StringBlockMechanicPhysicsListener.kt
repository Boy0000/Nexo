package com.nexomc.nexo.mechanics.custom_block.stringblock

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.utils.BlockHelpers
import com.nexomc.nexo.utils.SchedulerUtils
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.inventory.ItemStack
import java.util.function.Consumer

class StringBlockMechanicPhysicsListener : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun BlockPhysicsEvent.tripwireEvent() {
        if (changedType != Material.TRIPWIRE) return
        if (sourceBlock === block) return
        isCancelled = true

        BlockFace.entries.forEach { f: BlockFace ->
            if (!f.isCartesian || f.modY != 0 || f == BlockFace.SELF) return@forEach  // Only take N/S/W/E

            val changed = block.getRelative(f)
            if (changed.type != Material.TRIPWIRE) return@forEach

            val data = changed.blockData.clone()
            Bukkit.getScheduler().runTaskLater(
                NexoPlugin.instance(), Consumer { changed.setBlockData(data, false) }, 1L
            )
        }

        // Stores the pre-change blockdata and applies it on next tick to prevent the block from updating
        val blockData = block.blockData.clone()
        Bukkit.getScheduler().runTaskLater(NexoPlugin.instance(), Consumer {
            if (block.type.isAir()) return@Consumer
            block.setBlockData(blockData, false)
        }, 1L)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun BlockPistonExtendEvent.onPistonPush() {
        val tripwireList = getBlocks().filter { it.type == Material.TRIPWIRE }

        tripwireList.forEach { block: Block ->
            val mechanic = NexoBlocks.stringMechanic(block) ?: return@forEach
            block.setType(Material.AIR, false)
            mechanic.breakable.drop.spawns(block.location, ItemStack(Material.AIR))
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun BlockBreakEvent.onBreak() {
        val blockAbove = block.getRelative(BlockFace.UP)

        for (face: BlockFace in BlockFace.entries) {
            if (face == BlockFace.SELF && !face.isCartesian) continue
            if (block.type == Material.TRIPWIRE || block.type == Material.NOTE_BLOCK) break
            if (NexoFurniture.isFurniture(block.location)) break
            if (block.getRelative(face).type != Material.TRIPWIRE) continue

            if (player.gameMode != GameMode.CREATIVE) block.breakNaturally(player.inventory.itemInMainHand, true)
            else block.type = Material.AIR
            if (BlockHelpers.isReplaceable(blockAbove.type)) blockAbove.breakNaturally(true)
            SchedulerUtils.runTaskLater(1L) {
                StringMechanicHelpers.fixClientsideUpdate(block.location)
            }
        }
    }
}
