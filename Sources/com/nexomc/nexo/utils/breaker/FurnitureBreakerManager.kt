package com.nexomc.nexo.utils.breaker

import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.api.events.furniture.NexoFurnitureBreakEvent
import com.nexomc.nexo.api.events.furniture.NexoFurnitureDamageEvent
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.nms.NMSHandlers
import com.nexomc.nexo.utils.BlockHelpers.playCustomBlockSound
import com.nexomc.nexo.utils.EventUtils.call
import com.nexomc.nexo.utils.SchedulerUtils
import com.nexomc.nexo.utils.ticks
import com.nexomc.protectionlib.ProtectionLib
import kotlinx.coroutines.Job
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

object FurnitureBreakerManager {
    private val activeBreakerDataMap = ConcurrentHashMap<UUID, ActiveBreakerData>()

    fun startFurnitureBreak(player: Player, baseEntity: ItemDisplay, mechanic: FurnitureMechanic, block: Block) {
        stopFurnitureBreak(player)

        if (!NexoFurnitureDamageEvent(mechanic, baseEntity, player).call()) return

        val breakTime = mechanic.breakable.breakTime(player)
        //NMSHandlers.handler().playerUtils().applyMiningEffect(player)

        activeBreakerDataMap[player.uniqueId] = ActiveBreakerData(
            player, block.location, baseEntity.uniqueId, mechanic, breakTime, 0,
            createBreakScheduler(breakTime.toDouble(), player.uniqueId, baseEntity, mechanic),
            createBreakSoundScheduler(player.uniqueId)
        )
    }

    fun stopFurnitureBreak(player: Player) {
        val activeBreakerData = activeBreakerDataMap[player.uniqueId] ?: return

        activeBreakerData.cancelTasks()
        activeBreakerDataMap.remove(player.uniqueId)
        if (!player.isOnline) return
        NMSHandlers.handler().playerUtils().removeMiningEffect(player)
        activeBreakerData.resetProgress()
        activeBreakerData.sendBreakProgress()
    }

    private fun createBreakScheduler(
        blockBreakTime: Double,
        breakerUUID: UUID,
        baseEntity: ItemDisplay,
        mechanic: FurnitureMechanic
    ): Job {
        return SchedulerUtils.launchRepeating(activeBreakerDataMap[breakerUUID]!!.location, 1.ticks, 4.ticks) {
            val activeBreakerData = activeBreakerDataMap[breakerUUID] ?: return@launchRepeating
            val player = activeBreakerData.breaker
            val block = activeBreakerData.location.block

            when {
                !player.isOnline -> stopFurnitureBreak(player)
                activeBreakerData.mechanic != mechanic -> stopFurnitureBreak(player)
                mechanic.itemID != activeBreakerData.mechanic.itemID ->
                    stopFurnitureBreak(player)
                !activeBreakerData.isBroken -> {
                    activeBreakerData.addBreakTimeProgress(blockBreakTime / mechanic.breakable.breakTime(player))
                    activeBreakerData.sendBreakProgress()
                }
                NexoFurnitureBreakEvent(mechanic, baseEntity, player).call() && ProtectionLib.canBreak(player, block.location) -> {
                    activeBreakerData.resetProgress()
                    activeBreakerData.sendBreakProgress()

                    activeBreakerDataMap.values.asSequence()
                        .filter { it.breaker.uniqueId != breakerUUID && it.location == activeBreakerData.location }
                        .forEach { stopFurnitureBreak(it.breaker) }

                    NexoFurniture.remove(baseEntity, player)
                    activeBreakerData.cancelTasks()
                    activeBreakerDataMap.remove(breakerUUID)
                }
                else -> stopFurnitureBreak(player)
            }
        }
    }

    private fun createBreakSoundScheduler(breakerUUID: UUID): Job {
        return SchedulerUtils.launchRepeating(activeBreakerDataMap[breakerUUID]!!.location, 0.ticks, 4.ticks) {
            val activeBreakerData = activeBreakerDataMap[breakerUUID] ?: return@launchRepeating
            val player = activeBreakerData.breaker
            val baseEntity = Bukkit.getEntity(activeBreakerData.baseUUID) ?: return@launchRepeating stopFurnitureBreak(player)
            val mechanic = NexoFurniture.furnitureMechanic(baseEntity) ?: return@launchRepeating stopFurnitureBreak(player)
            val hitSound = mechanic.blockSounds

            when {
                !player.isOnline || mechanic.itemID != activeBreakerData.mechanic.itemID -> activeBreakerData.breakerSoundTask!!.cancel()
                hitSound?.hitSound == null -> activeBreakerData.breakerSoundTask!!.cancel()
                else -> playCustomBlockSound(baseEntity.location, hitSound.hitSound, hitSound.hitVolume, hitSound.hitPitch)
            }
        }
    }

    class ActiveBreakerData(
        val breaker: Player,
        val location: Location,
        val baseUUID: UUID,
        val mechanic: FurnitureMechanic,
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
            this.breakTimeProgress = min(this.breakTimeProgress + breakTimeProgress, totalBreakTime.toDouble())
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

    private val SOURCE_RANDOM = Random()
}