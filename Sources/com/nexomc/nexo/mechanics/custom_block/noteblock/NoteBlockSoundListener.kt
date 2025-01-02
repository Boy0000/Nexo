package com.nexomc.nexo.mechanics.custom_block.noteblock

import com.nexomc.nexo.utils.to
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.events.custom_block.noteblock.NexoNoteBlockBreakEvent
import com.nexomc.nexo.api.events.custom_block.noteblock.NexoNoteBlockPlaceEvent
import com.nexomc.nexo.utils.BlockHelpers.entityStandingOn
import com.nexomc.nexo.utils.BlockHelpers.isLoaded
import com.nexomc.nexo.utils.BlockHelpers.playCustomBlockSound
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.blocksounds.BlockSounds
import io.th0rgal.protectionlib.ProtectionLib
import org.bukkit.*
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageAbortEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.world.GenericGameEvent
import org.bukkit.event.world.WorldUnloadEvent
import org.bukkit.scheduler.BukkitTask

class NoteBlockSoundListener : Listener {
    private val breakerPlaySound = mutableMapOf<Location, BukkitTask>()

    @EventHandler
    fun WorldUnloadEvent.onWorldUnload() {
        breakerPlaySound.entries.forEach { (loc, task) ->
            if (loc.isWorldLoaded() || task.isCancelled) return@forEach
            task.cancel()
            breakerPlaySound.remove(loc)
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun BlockPlaceEvent.onPlacingWood() {
        if (blockPlaced.blockData.soundGroup.placeSound != Sound.BLOCK_WOOD_PLACE) return
        if (NexoBlocks.isNexoNoteBlock(blockPlaced)) return

        // Play sound for wood
        playCustomBlockSound(
            blockPlaced.location,
            BlockSounds.VANILLA_WOOD_PLACE,
            BlockSounds.VANILLA_PLACE_VOLUME,
            BlockSounds.VANILLA_PLACE_PITCH
        )
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun BlockBreakEvent.onBreakingWood() {
        val location = block.location

        breakerPlaySound[location]?.cancel()
        if (block.blockData.soundGroup.breakSound != Sound.BLOCK_WOOD_BREAK) return
        if (NexoBlocks.isNexoNoteBlock(block)) return
        if (isCancelled || !ProtectionLib.canBreak(player, location)) return

        playCustomBlockSound(
            location,
            BlockSounds.VANILLA_WOOD_BREAK,
            BlockSounds.VANILLA_BREAK_VOLUME,
            BlockSounds.VANILLA_BREAK_PITCH
        )
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun BlockDamageEvent.onHitWood() {
        val location = block.location
        val soundGroup = block.blockData.soundGroup

        if (soundGroup.hitSound != Sound.BLOCK_WOOD_HIT) return
        if (NexoBlocks.isNexoNoteBlock(block) || location in breakerPlaySound) return

        val task = Bukkit.getScheduler().runTaskTimer(
            NexoPlugin.instance(),
            Runnable {
                playCustomBlockSound(
                    location,
                    BlockSounds.VANILLA_WOOD_HIT,
                    BlockSounds.VANILLA_HIT_VOLUME,
                    BlockSounds.VANILLA_HIT_PITCH
                )
            }, 2L, 4L
        )

        breakerPlaySound[location] = task
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun BlockDamageAbortEvent.onStopHittingWood() {
        breakerPlaySound.remove(block.location)?.cancel()
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun GenericGameEvent.onStepFall() {
        val entity = entity as? LivingEntity ?: return
        if (!isLoaded(entity.location)) return
        if (event == GameEvent.HIT_GROUND && entity.fallDistance < 4.0) return
        val silent = entity.isInWater || entity.isSwimming || (VersionUtil.isPaperServer && (entity.isSneaking || entity.isInLava))
        if (event == GameEvent.STEP && silent) return

        val blockStandingOn = entityStandingOn(entity)?.takeUnless { it.type.isAir } ?: return
        if (blockStandingOn.blockData.soundGroup.stepSound != Sound.BLOCK_WOOD_STEP) return
        val mechanic = NexoBlocks.noteBlockMechanic(blockStandingOn)

        val (sound, volume, pitch) = when {
            event === GameEvent.STEP ->
                (mechanic?.blockSounds?.let { it.stepSound to it.stepVolume to it.stepPitch })
                    ?: (BlockSounds.VANILLA_WOOD_STEP to BlockSounds.VANILLA_STEP_VOLUME to BlockSounds.VANILLA_STEP_PITCH)
            event == GameEvent.HIT_GROUND ->
                (mechanic?.blockSounds?.let { it.fallSound to it.fallVolume to it.fallPitch })
                    ?: (BlockSounds.VANILLA_WOOD_FALL to BlockSounds.VANILLA_FALL_VOLUME to BlockSounds.VANILLA_FALL_PITCH)
            else -> return
        }

        playCustomBlockSound(entity.location, sound, SoundCategory.PLAYERS, volume, pitch)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun NexoNoteBlockPlaceEvent.onPlacing() {
        val blockSounds = mechanic.blockSounds?.takeIf(BlockSounds::hasPlaceSound) ?: return
        playCustomBlockSound(block.location, blockSounds.placeSound, blockSounds.placeVolume, blockSounds.placePitch)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun NexoNoteBlockBreakEvent.onBreaking() {
        val blockSounds = mechanic.blockSounds?.takeIf(BlockSounds::hasBreakSound) ?: return
        playCustomBlockSound(block.location, blockSounds.breakSound, blockSounds.breakVolume, blockSounds.breakPitch)
    }
}
