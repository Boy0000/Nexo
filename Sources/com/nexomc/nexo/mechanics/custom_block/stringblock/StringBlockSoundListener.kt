package com.nexomc.nexo.mechanics.custom_block.stringblock

import com.nexomc.nexo.utils.to
import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.events.custom_block.stringblock.NexoStringBlockBreakEvent
import com.nexomc.nexo.api.events.custom_block.stringblock.NexoStringBlockPlaceEvent
import com.nexomc.nexo.utils.BlockHelpers.isLoaded
import com.nexomc.nexo.utils.BlockHelpers.playCustomBlockSound
import com.nexomc.nexo.utils.blocksounds.BlockSounds
import org.bukkit.GameEvent
import org.bukkit.Material
import org.bukkit.SoundCategory
import org.bukkit.block.Block
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.world.GenericGameEvent

class StringBlockSoundListener : Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun BlockPistonExtendEvent.onPistonPush() {
        blocks.filter { it.type == Material.TRIPWIRE }.forEach { block: Block ->
            block.setType(Material.AIR, false)
            val mechanic = NexoBlocks.stringMechanic(block) ?: return@forEach
            val blockSounds = mechanic.blockSounds?.takeIf(BlockSounds::hasBreakSound) ?: return@forEach

            playCustomBlockSound(block.location, blockSounds.breakSound, blockSounds.breakVolume, blockSounds.breakPitch)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun NexoStringBlockPlaceEvent.onPlaceString() {
        val blockSounds = mechanic.blockSounds?.takeIf(BlockSounds::hasPlaceSound) ?: return

        playCustomBlockSound(block.location, blockSounds.placeSound, blockSounds.placeVolume, blockSounds.placePitch)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun NexoStringBlockBreakEvent.onBreakString() {
        val blockSounds = mechanic.blockSounds?.takeIf(BlockSounds::hasBreakSound) ?: return

        playCustomBlockSound(
            block.location,
            blockSounds.breakSound,
            blockSounds.breakVolume,
            blockSounds.breakPitch
        )
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun GenericGameEvent.onStepFall() {
        val entity = (entity as? LivingEntity)?.takeIf { isLoaded(it.location) } ?: return

        if (entity.lastDamageCause?.cause != EntityDamageEvent.DamageCause.FALL || event == GameEvent.HIT_GROUND) return
        val blockSounds = NexoBlocks.stringMechanic(entity.location.block)?.blockSounds ?: return

        val (sound, volume, pitch) = when {
            event == GameEvent.STEP && blockSounds.hasStepSound() ->
                blockSounds.stepSound to blockSounds.stepVolume to blockSounds.stepPitch

            event == GameEvent.HIT_GROUND && blockSounds.hasFallSound() ->
                blockSounds.fallSound to blockSounds.fallVolume to blockSounds.fallPitch

            else -> return
        }
        playCustomBlockSound(entity.location, sound, SoundCategory.PLAYERS, volume, pitch)
    }
}
