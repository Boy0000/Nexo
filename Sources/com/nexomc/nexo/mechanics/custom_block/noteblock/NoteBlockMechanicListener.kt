package com.nexomc.nexo.mechanics.custom_block.noteblock

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.events.custom_block.noteblock.NexoNoteBlockInteractEvent
import com.nexomc.nexo.mechanics.custom_block.CustomBlockHelpers
import com.nexomc.nexo.mechanics.storage.StorageType
import com.nexomc.nexo.utils.BlockHelpers.isInteractable
import com.nexomc.nexo.utils.EventUtils.call
import org.bukkit.Instrument
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.FallingBlock
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.player.PlayerInteractEvent

class NoteBlockMechanicListener : Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun PlayerInteractEvent.onPlaceAgainstNoteBlock() {
        val (block, hand) = (clickedBlock?.takeIf { it.type == Material.NOTE_BLOCK } ?: return) to (hand ?: return)

        if (action != Action.RIGHT_CLICK_BLOCK) return
        if (!player.isSneaking && isInteractable(block)) return
        if (useInteractedBlock() == Event.Result.DENY || !NexoBlocks.isNexoNoteBlock(block)) return

        val type = item?.type?.takeUnless { it.isAir || NexoBlocks.isNexoNoteBlock(item) } ?: return
        setUseInteractedBlock(Event.Result.DENY)
        val newData = if (type.isBlock) type.createBlockData() else null
        CustomBlockHelpers.makePlayerPlaceBlock(player, hand, item!!, block, blockFace, null, newData)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun NexoNoteBlockInteractEvent.onInteract() {
        val storageMechanic = mechanic.storage() ?: return

        when (storageMechanic.storageType) {
            StorageType.STORAGE, StorageType.SHULKER -> storageMechanic.openStorage(block, player)
            StorageType.PERSONAL -> storageMechanic.openPersonalStorage(player, block.location, null)
            StorageType.DISPOSAL -> storageMechanic.openDisposal(player, block.location, null)
            StorageType.ENDERCHEST -> player.openInventory(player.enderChest)
        }
        isCancelled = true
    }

    // If block is not a custom block, play the correct sound according to the below block or default
    @EventHandler(priority = EventPriority.NORMAL)
    fun NotePlayEvent.onNotePlayed() {
        if (NexoBlocks.isNexoNoteBlock(block)) isCancelled = true
        else if (NoteBlockMechanicFactory.instance()?.reimplementNoteblockFeatures == true) {
            val blockType = block.getRelative(BlockFace.DOWN).type.name.lowercase()

            val fakeInstrument = instrumentMap.entries.firstOrNull { blockType in it.value }?.key ?: Instrument.PIANO
            // This is deprecated, but seems to be without reason
            instrument = fakeInstrument
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun EntityChangeBlockEvent.onFallingNexoBlock() {
        val fallingBlock = entity as? FallingBlock ?: return
        val mechanic = NexoBlocks.noteBlockMechanic(fallingBlock.blockData) ?: return

        NexoBlocks.place(mechanic.itemID, block.location)
        fallingBlock.dropItem = false
    }

    @EventHandler
    fun BlockBreakEvent.onBreakBeneathFallingNexoBlock() {
        NoteMechanicHelpers.handleFallingNexoBlockAbove(block)
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun PlayerInteractEvent.onSetFire() {
        val (block, item) = (clickedBlock?.takeIf { it.type == Material.NOTE_BLOCK } ?: return) to (item ?: return)
        if (action != Action.RIGHT_CLICK_BLOCK || blockFace != BlockFace.UP) return

        val mechanic = NexoBlocks.noteBlockMechanic(block) ?: return
        if (!mechanic.canIgnite()) return
        if (item.type != Material.FLINT_AND_STEEL && item.type != Material.FIRE_CHARGE) return

        BlockIgniteEvent(block, BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL, player).call()
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun BlockIgniteEvent.onCatchFire() {
        val mechanic = NexoBlocks.noteBlockMechanic(block) ?: return
        if (!mechanic.canIgnite()) isCancelled = true
        else {
            block.world.playSound(block.location, Sound.ITEM_FLINTANDSTEEL_USE, 1f, 1f)
            block.getRelative(BlockFace.UP).type = Material.FIRE
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun BlockPistonExtendEvent.onPistonPush() {
        blocks.map(Block::getLocation).forEach(NoteMechanicHelpers::checkNoteBlockAbove)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun BlockPistonRetractEvent.onPistonPull() {
        blocks.map { it.getRelative(direction).location }.forEach(NoteMechanicHelpers::checkNoteBlockAbove)
    }

    companion object {
        // Used to determine what instrument to use when playing a note depending on below block
        val instrumentMap = mutableMapOf<Instrument, List<String>>().apply {
            put(Instrument.BELL, listOf("gold_block"))
            put(Instrument.BASS_DRUM, listOf("stone", "netherrack", "bedrock", "observer", "coral", "obsidian", "anchor", "quartz"))
            put(Instrument.FLUTE, listOf("clay"))
            put(Instrument.CHIME, listOf("packed_ice"))
            put(Instrument.GUITAR, listOf("wool"))
            put(Instrument.XYLOPHONE, listOf("bone_block"))
            put(Instrument.IRON_XYLOPHONE, listOf("iron_block"))
            put(Instrument.COW_BELL, listOf("soul_sand"))
            put(Instrument.DIDGERIDOO, listOf("pumpkin"))
            put(Instrument.BIT, listOf("emerald_block"))
            put(Instrument.BANJO, listOf("hay_bale"))
            put(Instrument.PLING, listOf("glowstone"))
            put(Instrument.BASS_GUITAR, listOf("wood"))
            put(Instrument.SNARE_DRUM, listOf("sand", "gravel", "concrete_powder", "soul_soil"))
            put(Instrument.STICKS, listOf("glass", "sea_lantern", "beacon"))
        }
    }
}
