package com.nexomc.nexo.mechanics.custom_block

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.events.custom_block.chorusblock.NexoChorusBlockBreakEvent
import com.nexomc.nexo.api.events.custom_block.chorusblock.NexoChorusBlockPlaceEvent
import com.nexomc.nexo.api.events.custom_block.noteblock.NexoNoteBlockBreakEvent
import com.nexomc.nexo.api.events.custom_block.noteblock.NexoNoteBlockPlaceEvent
import com.nexomc.nexo.api.events.custom_block.stringblock.NexoStringBlockBreakEvent
import com.nexomc.nexo.api.events.custom_block.stringblock.NexoStringBlockPlaceEvent
import com.nexomc.nexo.utils.BlockHelpers
import com.nexomc.nexo.utils.BlockHelpers.isLoaded
import com.nexomc.nexo.utils.SchedulerUtils
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.blocksounds.BlockSounds
import com.nexomc.nexo.utils.wrappers.AttributeWrapper
import com.nexomc.protectionlib.ProtectionLib
import com.tcoded.folialib.wrapper.task.WrappedTask
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

class CustomBlockSoundListener(val customSounds: CustomBlockFactory.CustomBlockSounds) : Listener {
    companion object {
        val breakerPlaySound = Object2ObjectOpenHashMap<Location, WrappedTask>()
    }

    @EventHandler
    fun WorldUnloadEvent.onWorldUnload() {
        breakerPlaySound.entries.forEach { (loc, task) ->
            if (loc.isLoaded || task.isCancelled) return@forEach
            task.cancel()
            breakerPlaySound.remove(loc)
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun BlockPlaceEvent.onPlacingWood() {
        if (blockPlaced.blockData.soundGroup.placeSound != Sound.BLOCK_WOOD_PLACE) return
        if (NexoBlocks.isNexoNoteBlock(blockPlaced) || NexoBlocks.isNexoChorusBlock(blockPlaced)) return

        // Play sound for wood
        BlockHelpers.playCustomBlockSound(blockPlaced.location, BlockSounds.VANILLA_WOOD_PLACE, BlockSounds.VANILLA_PLACE_VOLUME, BlockSounds.VANILLA_PLACE_PITCH)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun BlockBreakEvent.onBreakingWood() {
        val location = block.location

        breakerPlaySound[location]?.cancel()
        if (block.blockData.soundGroup.breakSound != Sound.BLOCK_WOOD_BREAK) return
        if (NexoBlocks.isNexoNoteBlock(block) || NexoBlocks.isNexoChorusBlock(block)) return
        if (isCancelled || !ProtectionLib.canBreak(player, location)) return

        BlockHelpers.playCustomBlockSound(location, BlockSounds.VANILLA_WOOD_BREAK, BlockSounds.VANILLA_BREAK_VOLUME, BlockSounds.VANILLA_BREAK_PITCH)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun BlockDamageEvent.onHitWood() {
        if (VersionUtil.below("1.20.5") || block.blockData.soundGroup.hitSound != Sound.BLOCK_WOOD_HIT) return
        val location = block.location.takeUnless { it in breakerPlaySound } ?: return
        val blockSounds = NexoBlocks.customBlockMechanic(block)?.blockSounds

        val sound = blockSounds?.hitSound ?: BlockSounds.VANILLA_WOOD_HIT
        val volume = blockSounds?.hitVolume ?: BlockSounds.VANILLA_HIT_VOLUME
        val pitch = blockSounds?.hitPitch ?: BlockSounds.VANILLA_HIT_PITCH

        breakerPlaySound[location] = SchedulerUtils.foliaScheduler.runAtLocationTimer(
            location, Runnable {
                BlockHelpers.playCustomBlockSound(location, sound, volume, pitch)
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
        if (event == GameEvent.HIT_GROUND && entity.fallDistance < (AttributeWrapper.SAFE_FALL_DISTANCE?.let(entity::getAttribute)?.value ?: 4.0)) return
        if (event == GameEvent.STEP && (entity.isInWater || entity.isSwimming || entity.isSneaking || entity.isInLava)) return

        val blockStandingOn = BlockHelpers.entityStandingOn(entity)?.takeUnless { it.type.isAir } ?: return
        if (blockStandingOn.blockData.soundGroup.stepSound != Sound.BLOCK_WOOD_STEP) return
        val mechanic = NexoBlocks.customBlockMechanic(blockStandingOn)

        val (sound, volume, pitch) = when {
            event === GameEvent.STEP -> mechanic?.blockSounds?.step ?: BlockSounds.WOOD_STEP
            event == GameEvent.HIT_GROUND -> mechanic?.blockSounds?.fall ?: BlockSounds.WOOD_FALL
            else -> return
        }

        BlockHelpers.playCustomBlockSound(entity.location, sound, SoundCategory.PLAYERS, volume, pitch)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun NexoNoteBlockPlaceEvent.onPlacing() {
        val blockSounds = mechanic.blockSounds?.takeIf(BlockSounds::hasPlaceSound) ?: return
        BlockHelpers.playCustomBlockSound(block.location, blockSounds.placeSound, blockSounds.placeVolume, blockSounds.placePitch)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun NexoNoteBlockBreakEvent.onBreaking() {
        val blockSounds = mechanic.blockSounds?.takeIf(BlockSounds::hasBreakSound) ?: return
        BlockHelpers.playCustomBlockSound(block.location, blockSounds.breakSound, blockSounds.breakVolume, blockSounds.breakPitch)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun NexoChorusBlockPlaceEvent.onPlacing() {
        val blockSounds = mechanic.blockSounds?.takeIf(BlockSounds::hasPlaceSound) ?: return
        BlockHelpers.playCustomBlockSound(block.location, blockSounds.placeSound, blockSounds.placeVolume, blockSounds.placePitch)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun NexoChorusBlockBreakEvent.onBreaking() {
        val blockSounds = mechanic.blockSounds?.takeIf(BlockSounds::hasBreakSound) ?: return
        BlockHelpers.playCustomBlockSound(block.location, blockSounds.breakSound, blockSounds.breakVolume, blockSounds.breakPitch)
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun NexoStringBlockPlaceEvent.onPlaceString() {
        val blockSounds = mechanic.blockSounds?.takeIf(BlockSounds::hasPlaceSound) ?: return

        BlockHelpers.playCustomBlockSound(block.location, blockSounds.placeSound, blockSounds.placeVolume, blockSounds.placePitch)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun NexoStringBlockBreakEvent.onBreakString() {
        val blockSounds = mechanic.blockSounds?.takeIf(BlockSounds::hasBreakSound) ?: return

        BlockHelpers.playCustomBlockSound(block.location, blockSounds.breakSound, blockSounds.breakVolume, blockSounds.breakPitch)
    }
}