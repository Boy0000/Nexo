package com.nexomc.nexo.mechanics.custom_block.noteblock

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.events.custom_block.noteblock.NexoNoteBlockBreakEvent
import com.nexomc.nexo.api.events.custom_block.noteblock.NexoNoteBlockPlaceEvent
import com.nexomc.nexo.mechanics.custom_block.CustomBlockFactory
import com.nexomc.nexo.utils.BlockHelpers.entityStandingOn
import com.nexomc.nexo.utils.BlockHelpers.isLoaded
import com.nexomc.nexo.utils.BlockHelpers.playCustomBlockSound
import com.nexomc.nexo.utils.SchedulerUtils
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.blocksounds.BlockSounds
import com.nexomc.nexo.utils.to
import com.tcoded.folialib.wrapper.task.WrappedTask
import io.th0rgal.protectionlib.ProtectionLib
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import org.bukkit.GameEvent
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageAbortEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.world.GenericGameEvent
import org.bukkit.event.world.WorldUnloadEvent

class NoteBlockSoundListener(val customSounds: CustomBlockFactory.CustomBlockSounds) : Listener {
    companion object {
        val breakerPlaySound = Object2ObjectOpenHashMap<Location, WrappedTask>()
    }

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
        if (NexoBlocks.isNexoNoteBlock(blockPlaced) || NexoBlocks.isNexoChorusBlock(blockPlaced)) return

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
        if (NexoBlocks.isNexoNoteBlock(block) || NexoBlocks.isNexoChorusBlock(block)) return
        if (isCancelled || !ProtectionLib.canBreak(player, location)) return

        playCustomBlockSound(
            location,
            BlockSounds.VANILLA_WOOD_BREAK,
            BlockSounds.VANILLA_BREAK_VOLUME,
            BlockSounds.VANILLA_BREAK_PITCH
        )
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun BlockDamageEvent.onHitWood() {
        if (VersionUtil.below("1.20.5") || block.blockData.soundGroup.hitSound != Sound.BLOCK_WOOD_HIT) return
        val location = block.location.takeUnless { it in breakerPlaySound } ?: return
        val blockSounds = NexoBlocks.customBlockMechanic(block.blockData)?.blockSounds

        val sound = blockSounds?.hitSound ?: BlockSounds.VANILLA_WOOD_HIT
        val volume = blockSounds?.hitVolume ?: BlockSounds.VANILLA_HIT_VOLUME
        val pitch = blockSounds?.hitPitch ?: BlockSounds.VANILLA_HIT_PITCH

        breakerPlaySound[location] = SchedulerUtils.foliaScheduler.runAtLocationTimer(
            location, Runnable {
            playCustomBlockSound(location, sound, volume, pitch)
        }, 2L, 4L)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun BlockDamageAbortEvent.onStopHittingWood() {
        breakerPlaySound.remove(block.location)?.cancel()
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun GenericGameEvent.onStepFall() {
        val entity = entity as? LivingEntity ?: return
        if (entity !is Player && customSounds.playersOnly) return
        if (!entity.location.isLoaded || isAsynchronous) return
        if (event == GameEvent.HIT_GROUND && entity.fallDistance < 4.0) return
        if (event == GameEvent.STEP && (entity.isInWater || entity.isSwimming || entity.isSneaking || entity.isInLava)) return

        val blockStandingOn = entityStandingOn(entity)?.takeUnless { it.type.isAir } ?: return
        if (blockStandingOn.blockData.soundGroup.stepSound != Sound.BLOCK_WOOD_STEP) return
        val mechanic = NexoBlocks.customBlockMechanic(blockStandingOn.blockData)

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
