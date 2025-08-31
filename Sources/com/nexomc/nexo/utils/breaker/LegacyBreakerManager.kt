package com.nexomc.nexo.utils.breaker

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.events.custom_block.noteblock.NexoNoteBlockDamageEvent
import com.nexomc.nexo.api.events.custom_block.stringblock.NexoStringBlockDamageEvent
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteBlockMechanic
import com.nexomc.nexo.mechanics.custom_block.stringblock.StringBlockMechanic
import com.nexomc.nexo.nms.NMSHandlers
import com.nexomc.nexo.utils.BlockHelpers.playCustomBlockSound
import com.nexomc.nexo.utils.EventUtils.call
import com.nexomc.nexo.utils.ItemUtils.damageItem
import com.nexomc.nexo.utils.SchedulerUtils
import com.nexomc.nexo.utils.ticks
import com.nexomc.protectionlib.ProtectionLib
import kotlinx.coroutines.Job
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

class LegacyBreakerManager(private val activeBreakerDataMap: ConcurrentHashMap<UUID, ActiveBreakerData>) : BreakerManager {
    fun activeBreakerData(player: Player) = activeBreakerDataMap[player.uniqueId]

    override fun startBlockBreak(player: Player, block: Block, mechanic: CustomBlockMechanic) {
        stopBlockBreak(player)
        if (!when (mechanic) {
            is NoteBlockMechanic -> NexoNoteBlockDamageEvent(mechanic, block, player)
            is StringBlockMechanic -> NexoStringBlockDamageEvent(mechanic, block, player)
            else -> return
        }.call()) return

        val breakTime = mechanic.breakable.breakTime(player)
        NMSHandlers.handler().playerUtils().applyMiningEffect(player)

        activeBreakerDataMap[player.uniqueId] = ActiveBreakerData(
            player, block.location, mechanic, breakTime, 0,
            createBreakScheduler(breakTime.toDouble(), player.uniqueId),
            createBreakSoundScheduler(player.uniqueId)
        )
    }

    override fun stopBlockBreak(player: Player) {
        val activeBreakerData = activeBreakerDataMap[player.uniqueId] ?: return

        activeBreakerData.cancelTasks()
        activeBreakerDataMap.remove(player.uniqueId)
        if (!player.isOnline) return
        NMSHandlers.handler().playerUtils().removeMiningEffect(player)
        activeBreakerData.resetProgress()
        activeBreakerData.sendBreakProgress()
    }

    private fun createBreakScheduler(blockBreakTime: Double, breakerUUID: UUID): Job {
        return SchedulerUtils.launchRepeating(activeBreakerDataMap[breakerUUID]!!.location, 1.ticks, 1.ticks) {
            val activeBreakerData = activeBreakerDataMap[breakerUUID] ?: return@launchRepeating
            val player = activeBreakerData.breaker
            val block = activeBreakerData.location.block
            val mechanic = NexoBlocks.customBlockMechanic(block)
            val breakable = mechanic?.breakable

            when {
                !player.isOnline || breakable == null -> stopBlockBreak(player)
                activeBreakerData.mechanic != mechanic -> stopBlockBreak(player)
                mechanic.customVariation != activeBreakerData.mechanic.customVariation ->
                    stopBlockBreak(player)
                !activeBreakerData.isBroken -> {
                    activeBreakerData.addBreakTimeProgress(blockBreakTime / breakable.breakTime(player))
                    activeBreakerData.sendBreakProgress()
                }
                BlockBreakEvent(block, player).call() && ProtectionLib.canBreak(player, block.location) -> {
                    NMSHandlers.handler().playerUtils().removeMiningEffect(player)
                    activeBreakerData.resetProgress()
                    activeBreakerData.sendBreakProgress()

                    activeBreakerDataMap.values
                        .asSequence()
                        .filter { it.breaker.uniqueId != breakerUUID && it.location == activeBreakerData.location }
                        .forEach { stopBlockBreak(it.breaker) }

                    damageItem(player, player.inventory.itemInMainHand)
                    block.type = Material.AIR
                    activeBreakerData.cancelTasks()
                    activeBreakerDataMap.remove(breakerUUID)
                }
                else -> stopBlockBreak(player)
            }
        }
    }

    private fun createBreakSoundScheduler(breakerUUID: UUID): Job {
        return SchedulerUtils.launchRepeating(activeBreakerDataMap[breakerUUID]!!.location, 0.ticks, 40.ticks) {
            val activeBreakerData = activeBreakerDataMap[breakerUUID] ?: return@launchRepeating
            val player = activeBreakerData.breaker
            val block = activeBreakerData.location.block
            val mechanic = NexoBlocks.customBlockMechanic(block) ?: return@launchRepeating stopBlockBreak(player)
            val hitSound = mechanic.blockSounds

            when {
                !player.isOnline || mechanic.customVariation != activeBreakerData.mechanic.customVariation -> stopBlockBreak(player)
                hitSound?.hitSound == null -> activeBreakerData.breakerSoundTask!!.cancel()
                else -> playCustomBlockSound(block.location, hitSound.hitSound, hitSound.hitVolume, hitSound.hitPitch)
            }
        }
    }

    class ActiveBreakerData(
        val breaker: Player,
        val location: Location,
        val mechanic: CustomBlockMechanic,
        private val totalBreakTime: Int,
        breakTimeProgress: Int,
        private val breakerTask: Job?,
        val breakerSoundTask: Job?
    ) {
        private val sourceId = SOURCE_RANDOM.nextInt()
        private var breakTimeProgress: Double

        init {
            this.breakTimeProgress = breakTimeProgress.toDouble()
        }

        fun totalBreakTime(): Int {
            return totalBreakTime
        }

        fun breakTimeProgress(): Double {
            return breakTimeProgress
        }

        fun breakTimeProgress(breakTimeProgress: Double) {
            this.breakTimeProgress = breakTimeProgress
        }

        fun addBreakTimeProgress(breakTimeProgress: Double) {
            this.breakTimeProgress = min(
                this.breakTimeProgress + breakTimeProgress,
                totalBreakTime.toDouble()
            )
        }

        fun sendBreakProgress() {
            breaker.sendBlockDamage(location, calculateDamage(), sourceId)
        }

        fun calculateDamage(): Float {
            val percentage = this.breakTimeProgress / this.totalBreakTime
            return (MAX_DAMAGE * percentage).toFloat().coerceIn(MIN_DAMAGE, MAX_DAMAGE)
        }

        val isBroken: Boolean
            get() = breakTimeProgress >= this.totalBreakTime

        fun resetProgress() {
            this.breakTimeProgress = 0.0
        }

        fun cancelTasks() {
            breakerTask?.cancel()
            breakerSoundTask?.cancel()
        }

        companion object {
            const val MAX_DAMAGE = 1f
            const val MIN_DAMAGE = 0f
        }
    }

    companion object {
        private val SOURCE_RANDOM = Random()
    }
}
