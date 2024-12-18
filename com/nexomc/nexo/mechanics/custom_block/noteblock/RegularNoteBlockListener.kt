package com.nexomc.nexo.mechanics.custom_block.noteblock

import com.nexomc.nexo.api.NexoBlocks
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.block.NotePlayEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.meta.SkullMeta

class RegularNoteBlockListener : Listener {
    @EventHandler(priority = EventPriority.NORMAL)
    fun NotePlayEvent.onNotePlay() {
        isCancelled = true
        if (NexoBlocks.isNexoNoteBlock(block)) return

        val regularNoteBlock = RegularNoteBlock(block, null)
        regularNoteBlock.runClickAction(Action.LEFT_CLICK_BLOCK)
    }

    @EventHandler
    fun BlockPhysicsEvent.onNoteBlockPower() {
        if (block.type != Material.NOTE_BLOCK || NexoBlocks.isNexoNoteBlock(block)) return

        val regularNoteBlock = RegularNoteBlock(block, null)

        if (!block.isBlockIndirectlyPowered) {
            regularNoteBlock.setPowered(false)
        } else if (!regularNoteBlock.isPowered) {
            regularNoteBlock.playSoundNaturally()
            regularNoteBlock.setPowered(true)
        }
    }

    @EventHandler
    fun PlayerInteractEvent.onRightClickNoteBlock() {
        if (action != Action.RIGHT_CLICK_BLOCK) return
        val block = clickedBlock?.takeIf { it.type == Material.NOTE_BLOCK }?.takeUnless(NexoBlocks::isNexoNoteBlock) ?: return

        val (mainHandItem, offHandItem) = player.inventory.let { it.itemInMainHand to it.itemInOffHand }
        if (mainHandItem.itemMeta is SkullMeta && blockFace == BlockFace.UP) return

        val regularNoteBlock = RegularNoteBlock(block, player)
        if (player.isSneaking && (!mainHandItem.isEmpty || !offHandItem.isEmpty)) return

        setUseInteractedBlock(Event.Result.DENY)
        regularNoteBlock.runClickAction(Action.RIGHT_CLICK_BLOCK)
    }
}
