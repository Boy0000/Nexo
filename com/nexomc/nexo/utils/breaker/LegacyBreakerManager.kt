package com.nexomc.nexo.utils.breaker

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.api.events.custom_block.noteblock.NexoNoteBlockDamageEvent
import com.nexomc.nexo.api.events.custom_block.stringblock.NexoStringBlockDamageEvent
import com.nexomc.nexo.api.events.furniture.NexoFurnitureDamageEvent
import com.nexomc.nexo.mechanics.Mechanic
import com.nexomc.nexo.mechanics.breakable.BreakableMechanic
import com.nexomc.nexo.mechanics.custom_block.CustomBlockFactory
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteBlockMechanic
import com.nexomc.nexo.mechanics.custom_block.stringblock.StringBlockMechanic
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.nms.NMSHandlers
import com.nexomc.nexo.utils.BlockHelpers.playCustomBlockSound
import com.nexomc.nexo.utils.EventUtils.call
import com.nexomc.nexo.utils.ItemUtils.damageItem
import io.th0rgal.protectionlib.ProtectionLib
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.scheduler.BukkitTask
import java.util.*
import kotlin.math.min

class LegacyBreakerManager(private val activeBreakerDataMap: MutableMap<UUID, ActiveBreakerData>) :
    BreakerManager {
    fun activeBreakerData(player: Player) = activeBreakerDataMap[player.uniqueId]

    override fun startFurnitureBreak(
        player: Player,
        baseEntity: ItemDisplay,
        mechanic: FurnitureMechanic,
        block: Block
    ) {
        stopBlockBreak(player)

        if (!NexoFurnitureDamageEvent(mechanic, baseEntity, player).call()) return

        val breakTime = mechanic.breakable.breakTime(player)
        NMSHandlers.handler().applyMiningEffect(player)
        val activeBreakerData = ActiveBreakerData(
            player,
            block.location,
            mechanic,
            mechanic.breakable,
            breakTime,
            0,
            createBreakScheduler(breakTime.toDouble(), player.uniqueId),
            createBreakSoundScheduler(player.uniqueId)
        )
        activeBreakerDataMap[player.uniqueId] = activeBreakerData
    }

    override fun startBlockBreak(player: Player, block: Block, mechanic: CustomBlockMechanic) {
        stopBlockBreak(player)
        if (!when (mechanic) {
            is NoteBlockMechanic -> NexoNoteBlockDamageEvent(mechanic, block, player)
            is StringBlockMechanic -> NexoStringBlockDamageEvent(mechanic, block, player)
            else -> return
        }.call()) return

        val breakTime = mechanic.breakable.breakTime(player)
        NMSHandlers.handler().applyMiningEffect(player)
        val activeBreakerData = ActiveBreakerData(
            player,
            block.location,
            mechanic,
            mechanic.breakable,
            breakTime,
            0,
            createBreakScheduler(breakTime.toDouble(), player.uniqueId),
            createBreakSoundScheduler(player.uniqueId)
        )
        activeBreakerDataMap[player.uniqueId] = activeBreakerData
    }

    override fun stopBlockBreak(player: Player) {
        val activeBreakerData = activeBreakerDataMap[player.uniqueId] ?: return

        activeBreakerData.cancelTasks()
        activeBreakerDataMap.remove(player.uniqueId)
        if (!player.isOnline) return
        NMSHandlers.handler().removeMiningEffect(player)
        activeBreakerData.resetProgress()
        activeBreakerData.sendBreakProgress()
    }

    private fun createBreakScheduler(blockBreakTime: Double, breakerUUID: UUID): BukkitTask {
        return Bukkit.getScheduler().runTaskTimer(NexoPlugin.instance(), Runnable {
            val activeBreakerData = activeBreakerDataMap[breakerUUID] ?: return@Runnable
            val player = activeBreakerData.breaker
            val block = activeBreakerData.location.block
            val blockMechanic = NexoBlocks.customBlockMechanic(block.blockData)
            val furnitureMechanic = NexoFurniture.furnitureMechanic(block)
            val breakable = blockMechanic?.breakable ?: furnitureMechanic?.breakable
            when {
                !player.isOnline || breakable == null -> stopBlockBreak(player)
                activeBreakerData.mechanic != blockMechanic && activeBreakerData.mechanic != furnitureMechanic ->
                    stopBlockBreak(player)
                blockMechanic != null && activeBreakerData.mechanic is CustomBlockMechanic && blockMechanic.customVariation != activeBreakerData.mechanic.customVariation ->
                    stopBlockBreak(player)
                furnitureMechanic != null && activeBreakerData.mechanic is FurnitureMechanic && (activeBreakerData.mechanic.itemID != furnitureMechanic.itemID) ->
                    stopBlockBreak(player)
                !activeBreakerData.isBroken -> {
                    activeBreakerData.addBreakTimeProgress(blockBreakTime / breakable.breakTime(player))
                    activeBreakerData.sendBreakProgress()
                }
                BlockBreakEvent(block, player).call() && ProtectionLib.canBreak(player, block.location) -> {
                    NMSHandlers.handler().removeMiningEffect(player)
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
        }, 1, 1)
    }

    private fun createBreakSoundScheduler(breakerUUID: UUID): BukkitTask {
        return Bukkit.getScheduler().runTaskTimer(NexoPlugin.instance(), Runnable {
            val activeBreakerData = activeBreakerDataMap[breakerUUID] ?: return@Runnable
            val player = activeBreakerData.breaker
            val block = activeBreakerData.location.block
            val blockMechanic = NexoBlocks.customBlockMechanic(block.blockData)
            val furnitureMechanic = NexoFurniture.furnitureMechanic(block)

            when {
                !player.isOnline || blockMechanic == null && furnitureMechanic == null -> stopBlockBreak(player)
                blockMechanic != null -> when {
                    activeBreakerData.mechanic !is CustomBlockMechanic ->
                        stopBlockBreak(player)
                    blockMechanic.customVariation != activeBreakerData.mechanic.customVariation ->
                        stopBlockBreak(player)
                    !blockMechanic.hasBlockSounds() || !blockMechanic.blockSounds!!.hasHitSound() ->
                        activeBreakerData.breakerSoundTask!!.cancel()

                    else -> {
                        //TODO Allow for third party blocks to handle this somehow
                        var sound = ""
                        when {
                            blockMechanic.type === CustomBlockFactory.instance()?.NOTEBLOCK ->
                                sound = blockMechanic.blockSounds?.hitSound ?: "required.wood.hit"

                            blockMechanic.type === CustomBlockFactory.instance()?.STRINGBLOCK ->
                                sound = blockMechanic.blockSounds?.hitSound ?: "block.tripwire.detach"
                        }
                        playCustomBlockSound(block.location, sound, blockMechanic.blockSounds!!.hitVolume, blockMechanic.blockSounds!!.hitPitch)
                    }
                }
                else -> when {
                    activeBreakerData.mechanic !is FurnitureMechanic -> stopBlockBreak(player)
                    furnitureMechanic!!.itemID != activeBreakerData.mechanic.itemID ->
                        activeBreakerData.breakerSoundTask!!.cancel()

                    furnitureMechanic.hasBlockSounds() && furnitureMechanic.blockSounds!!.hasHitSound() -> {
                        val sound = furnitureMechanic.blockSounds.hitSound
                    }
                }
            }
        }, 0, 4L)
    }

    class ActiveBreakerData(
        val breaker: Player,
        val location: Location,
        val mechanic: Mechanic,
        private val breakable: BreakableMechanic,
        private val totalBreakTime: Int,
        breakTimeProgress: Int,
        private val breakerTask: BukkitTask?,
        val breakerSoundTask: BukkitTask?
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
