package com.nexomc.nexo.mechanics.furniture.listeners

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.api.events.furniture.NexoFurnitureBreakEvent
import com.nexomc.nexo.api.events.furniture.NexoFurniturePlaceEvent
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager
import com.nexomc.nexo.utils.BlockHelpers
import com.nexomc.nexo.utils.BlockHelpers.isLoaded
import com.nexomc.nexo.utils.SchedulerUtils
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.blocksounds.BlockSounds
import com.nexomc.nexo.utils.to
import com.nexomc.protectionlib.ProtectionLib
import com.tcoded.folialib.wrapper.task.WrappedTask
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import org.bukkit.*
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageAbortEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.world.GenericGameEvent
import org.bukkit.event.world.WorldUnloadEvent

class FurnitureSoundListener : Listener {
    companion object {
        val breakerPlaySound = Object2ObjectOpenHashMap<Location, WrappedTask>()
    }

    @EventHandler
    fun WorldUnloadEvent.onWorldUnload() {
        for ((location, task) in breakerPlaySound) {
            if (location.isLoaded || task.isCancelled) continue
            task.cancel()
            breakerPlaySound.remove(location)
        }
    }

    // Play sound due to furniture/barrier custom sound replacing stone
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun BlockPlaceEvent.onPlacingStone() {
        if (NexoBlocks.isNexoStringBlock(blockPlaced) || blockPlaced.isEmpty) return
        if (block.blockData.soundGroup.placeSound != Sound.BLOCK_STONE_PLACE) return

        BlockHelpers.playCustomBlockSound(block.location, BlockSounds.VANILLA_STONE_PLACE, BlockSounds.VANILLA_PLACE_VOLUME, BlockSounds.VANILLA_PLACE_PITCH)
    }

    // Play sound due to furniture/barrier custom sound replacing stone
    @EventHandler(priority = EventPriority.HIGH)
    fun BlockBreakEvent.onBreakingStone() {
        val location = block.location
        val mechanicBelow = NexoBlocks.stringMechanic(block.getRelative(BlockFace.DOWN))
        breakerPlaySound.remove(location)?.cancel()

        if (NexoBlocks.isNexoStringBlock(block) || block.type == Material.TRIPWIRE && mechanicBelow != null && mechanicBelow.isTall) return
        if (block.blockData.soundGroup.breakSound != Sound.BLOCK_STONE_BREAK) return
        if ((block.type == Material.BARRIER || block.isEmpty) && NexoFurniture.isFurniture(location)) return

        if (isCancelled || !ProtectionLib.canBreak(player, location)) return
        BlockHelpers.playCustomBlockSound(location, BlockSounds.VANILLA_STONE_BREAK, BlockSounds.VANILLA_BREAK_VOLUME, BlockSounds.VANILLA_BREAK_PITCH)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun BlockDamageEvent.onHitStone() {
        val location = block.location
        val soundGroup = block.blockData.soundGroup

        if (instaBreak || block.type == Material.BARRIER || soundGroup.hitSound != Sound.BLOCK_STONE_HIT) return
        if (location in breakerPlaySound) return

        breakerPlaySound[location] = SchedulerUtils.foliaScheduler.runAtLocationTimer(location, Runnable {
                BlockHelpers.playCustomBlockSound(location, BlockSounds.VANILLA_STONE_HIT, BlockSounds.VANILLA_HIT_VOLUME, BlockSounds.VANILLA_HIT_PITCH)
            }, 2L, 4L
        )
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun BlockDamageAbortEvent.onStopHittingStone() {
        breakerPlaySound.remove(block.location)?.cancel()
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun GenericGameEvent.onStepFall() {
        val player = entity?.takeIf { it.location.isLoaded && !isAsynchronous && !VersionUtil.isFoliaServer } as? Player ?: return
        val blockStandingOn = BlockHelpers.entityStandingOn(player)?.takeUnless { it.isEmpty } ?: return
        val (cause, soundGroup) = player.lastDamageCause to blockStandingOn.blockData.soundGroup

        if (event === GameEvent.HIT_GROUND && cause != null && cause.cause != EntityDamageEvent.DamageCause.FALL) return
        if (blockStandingOn.type == Material.TRIPWIRE) return
        val mechanic = NexoFurniture.furnitureMechanic(blockStandingOn.getRelative(BlockFace.UP).location)
        if (soundGroup.stepSound != Sound.BLOCK_STONE_STEP && mechanic == null) return
        if (mechanic != null && !IFurniturePacketManager.blockIsHitbox(blockStandingOn)) return

        val (sound, volume, pitch) = when {
            event === GameEvent.STEP ->
                (mechanic?.blockSounds?.let { it.stepSound to it.stepVolume to it.stepPitch })
                    ?: (BlockSounds.VANILLA_STONE_STEP to BlockSounds.VANILLA_STEP_VOLUME to BlockSounds.VANILLA_STEP_PITCH)
            event == GameEvent.HIT_GROUND ->
                (mechanic?.blockSounds?.let { it.fallSound to it.fallVolume to it.fallPitch })
                    ?: (BlockSounds.VANILLA_STONE_FALL to BlockSounds.VANILLA_FALL_VOLUME to BlockSounds.VANILLA_FALL_PITCH)
            else -> return
        }

        BlockHelpers.playCustomBlockSound(player.location, sound, SoundCategory.PLAYERS, volume, pitch)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun NexoFurniturePlaceEvent.onPlacingFurniture() {
        val blockSounds = mechanic.blockSounds?.takeIf(BlockSounds::hasPlaceSound) ?: return
        BlockHelpers.playCustomBlockSound(baseEntity.location, blockSounds.placeSound, blockSounds.placeVolume, blockSounds.placePitch)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun NexoFurnitureBreakEvent.onBreakingFurniture() {
        val blockSounds = mechanic.blockSounds?.takeIf(BlockSounds::hasBreakSound) ?: return
        BlockHelpers.playCustomBlockSound(baseEntity.location, blockSounds.breakSound, blockSounds.breakVolume, blockSounds.breakPitch)
    }
}
