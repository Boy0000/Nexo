package com.nexomc.nexo.mechanics.custom_block.noteblock

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.utils.BlockHelpers.isLoaded
import com.nexomc.nexo.utils.SchedulerUtils
import org.bukkit.Bukkit
import org.bukkit.GameEvent
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.type.NoteBlock
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.world.GenericGameEvent

class NoteBlockMechanicPhysicsListener : Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun BlockPistonExtendEvent.onPistonPush() {
        if (blocks.any { it.type == Material.NOTE_BLOCK }) isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun BlockPistonRetractEvent.onPistonPull() {
        if (blocks.any { it.type == Material.NOTE_BLOCK }) isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun BlockPhysicsEvent.onBlockPhysics() {
        val (aboveBlock, belowBlock) = block.getRelative(BlockFace.UP) to block.getRelative(BlockFace.DOWN)

        if (belowBlock.type == Material.NOTE_BLOCK) {
            isCancelled = true
            updateBlockAbove(belowBlock)
        } else if (aboveBlock.type == Material.NOTE_BLOCK) {
            isCancelled = true
            updateBlockAbove(aboveBlock)
        }
        if (block.type == Material.NOTE_BLOCK) {
            isCancelled = true
            updateBlockAbove(block)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun GenericGameEvent.onNoteblockPowered() {
        val block = location.block
        val data = block.blockData.clone() as? NoteBlock ?: return

        if (!isLoaded(location) || !isLoaded(block.location) || event !== GameEvent.NOTE_BLOCK_PLAY) return

        SchedulerUtils.foliaScheduler.runAtLocationLater(location, Runnable { block.setBlockData(data, false) }, 1L)
    }

    private fun updateBlockAbove(block: Block) {
        val (blockAbove, nextBlock) = block.getRelative(BlockFace.UP).let { it to it.getRelative(BlockFace.UP) }
        if (blockAbove.type == Material.NOTE_BLOCK) blockAbove.state.update(true, true)
        if (nextBlock.type == Material.NOTE_BLOCK) updateBlockAbove(blockAbove)
    }
}
