package com.nexomc.nexo.mechanics.furniture.listeners

import com.mineinabyss.idofront.util.to
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.api.events.furniture.NexoFurnitureBreakEvent
import com.nexomc.nexo.api.events.furniture.NexoFurniturePlaceEvent
import com.nexomc.nexo.utils.BlockHelpers.entityStandingOn
import com.nexomc.nexo.utils.BlockHelpers.isLoaded
import com.nexomc.nexo.utils.BlockHelpers.playCustomBlockSound
import com.nexomc.nexo.utils.blocksounds.BlockSounds
import io.th0rgal.protectionlib.ProtectionLib
import net.minecraft.references.Blocks
import org.bukkit.*
import org.bukkit.block.BlockFace
import org.bukkit.entity.LivingEntity
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
import org.bukkit.scheduler.BukkitTask

class FurnitureSoundListener : Listener {
    private val breakerPlaySound = mutableMapOf<Location, BukkitTask>()

    @EventHandler
    fun WorldUnloadEvent.onWorldUnload() {
        for ((location, task) in breakerPlaySound) {
            if (location.isWorldLoaded() || task.isCancelled) continue
            task.cancel()
            breakerPlaySound.remove(location)
        }
    }

    // Play sound due to furniture/barrier custom sound replacing stone
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun BlockPlaceEvent.onPlacingStone() {
        if (NexoBlocks.isNexoStringBlock(block)) return
        if (block.blockData.soundGroup.placeSound != Sound.BLOCK_STONE_PLACE) return

        playCustomBlockSound(
            block.location,
            BlockSounds.VANILLA_STONE_PLACE,
            BlockSounds.VANILLA_PLACE_VOLUME,
            BlockSounds.VANILLA_PLACE_PITCH
        )
    }

    // Play sound due to furniture/barrier custom sound replacing stone
    @EventHandler(priority = EventPriority.HIGH)
    fun BlockBreakEvent.onBreakingStone() {
        val location = block.location
        val mechanicBelow = NexoBlocks.stringMechanic(block.getRelative(BlockFace.DOWN))
        breakerPlaySound.remove(location)?.cancel()

        if (NexoBlocks.isNexoStringBlock(block) || block.type == Material.TRIPWIRE && mechanicBelow != null && mechanicBelow.isTall) return
        if (block.blockData.soundGroup.breakSound != Sound.BLOCK_STONE_BREAK) return
        if (NexoFurniture.isFurniture(location) && block.type == Material.BARRIER || block.isEmpty) return

        if (isCancelled || !ProtectionLib.canBreak(player, location)) return
        playCustomBlockSound(
            location,
            BlockSounds.VANILLA_STONE_BREAK,
            BlockSounds.VANILLA_BREAK_VOLUME,
            BlockSounds.VANILLA_BREAK_PITCH
        )
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun BlockDamageEvent.onHitStone() {
        val location = block.location
        val soundGroup = block.blockData.soundGroup

        if (instaBreak || block.type == Material.BARRIER || soundGroup.hitSound != Sound.BLOCK_STONE_HIT) return
        if (breakerPlaySound.containsKey(location)) return

        breakerPlaySound[location] = Bukkit.getScheduler().runTaskTimer(
            NexoPlugin.instance(),
            Runnable {
                playCustomBlockSound(
                    location,
                    BlockSounds.VANILLA_STONE_HIT,
                    BlockSounds.VANILLA_HIT_VOLUME,
                    BlockSounds.VANILLA_HIT_PITCH
                )
            }, 2L, 4L
        )
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun BlockDamageAbortEvent.onStopHittingStone() {
        breakerPlaySound.remove(block.location)?.cancel()
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun GenericGameEvent.onStepFall() {
        val entity = entity as? LivingEntity ?: return
        if (!isLoaded(entity.location)) return

        val blockStandingOn = entityStandingOn(entity)?.takeUnless { it.type.isAir } ?: return
        val (cause, soundGroup) = entity.lastDamageCause to blockStandingOn.blockData.soundGroup

        if (soundGroup.stepSound != Sound.BLOCK_STONE_STEP) return
        if (event === GameEvent.HIT_GROUND && cause != null && cause.cause != EntityDamageEvent.DamageCause.FALL) return
        if (blockStandingOn.type == Material.TRIPWIRE) return
        val mechanic = NexoFurniture.furnitureMechanic(blockStandingOn.location)

        val (sound, volume, pitch) = when {
            event === GameEvent.STEP ->
                (mechanic?.blockSounds?.let { it.stepSound to it.stepVolume to it.stepPitch })
                    ?: (BlockSounds.VANILLA_STONE_STEP to BlockSounds.VANILLA_STEP_VOLUME to BlockSounds.VANILLA_STEP_PITCH)
            event == GameEvent.HIT_GROUND ->
                (mechanic?.blockSounds?.let { it.fallSound to it.fallVolume to it.fallPitch })
                    ?: (BlockSounds.VANILLA_STONE_FALL to BlockSounds.VANILLA_FALL_VOLUME to BlockSounds.VANILLA_FALL_PITCH)
            else -> return
        }

        playCustomBlockSound(entity.location, sound, SoundCategory.PLAYERS, volume, pitch)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun NexoFurniturePlaceEvent.onPlacingFurniture() {
        val blockSounds = mechanic.blockSounds?.takeIf(BlockSounds::hasPlaceSound) ?: return
        playCustomBlockSound(baseEntity.location, blockSounds.placeSound, blockSounds.placeVolume, blockSounds.placePitch)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun NexoFurnitureBreakEvent.onBreakingFurniture() {
        val blockSounds = mechanic.blockSounds?.takeIf(BlockSounds::hasBreakSound) ?: return
        playCustomBlockSound(baseEntity.location, blockSounds.breakSound, blockSounds.breakVolume, blockSounds.breakPitch)
    }
}
