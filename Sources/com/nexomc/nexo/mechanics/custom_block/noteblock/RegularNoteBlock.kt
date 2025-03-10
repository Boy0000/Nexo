package com.nexomc.nexo.mechanics.custom_block.noteblock

import com.jeff_media.customblockdata.CustomBlockData
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.nms.NMSHandlers
import com.nexomc.nexo.utils.BlockHelpers.toCenterBlockLocation
import org.bukkit.GameEvent
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Skull
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import kotlin.math.pow

class RegularNoteBlock(private val block: Block, private val player: Player?) {
    private val blockAbove: Block = block.getRelative(BlockFace.UP)
    private val noteKey: NamespacedKey = NamespacedKey(NexoPlugin.instance(), "note")
    private val poweredKey: NamespacedKey = NamespacedKey(NexoPlugin.instance(), "powered")
    private val container: PersistentDataContainer = CustomBlockData(block, NexoPlugin.instance())
    val isPowered: Boolean = container.getOrDefault(poweredKey, PersistentDataType.BOOLEAN, false)
    val note: Byte = container.getOrDefault(noteKey, PersistentDataType.BYTE, 0.toByte())
    val pitch: Float = 2.0.pow(((note - 12f) / 12f).toDouble()).toFloat()
    val sound: String? = when (blockAbove.type) {
        Material.SKELETON_SKULL -> "block.note_block.imitate.skeleton"
        Material.PIGLIN_HEAD -> "block.note_block.imitate.piglin"
        Material.ZOMBIE_HEAD -> "block.note_block.imitate.zombie"
        Material.CREEPER_HEAD -> "block.note_block.imitate.creeper"
        Material.DRAGON_HEAD -> "block.note_block.imitate.ender_dragon"
        Material.WITHER_SKELETON_SKULL -> "block.note_block.imitate.wither_skeleton"
        Material.PLAYER_HEAD -> (blockAbove.state as Skull).noteBlockSound?.value()

        else -> {
            val blockBelow = block.getRelative(BlockFace.DOWN)
            val noteBlockMechanic = NexoBlocks.noteBlockMechanic(blockBelow)
            (noteBlockMechanic?.instrument ?: "block.note_block.${NMSHandlers.handler().noteBlockInstrument(blockBelow)}").lowercase()
        }
    }

    fun runClickAction(action: Action) {
        playSoundNaturally()
        if (action == Action.RIGHT_CLICK_BLOCK) increaseNote()
    }

    fun playSoundNaturally() {
        if (!blockAbove.isEmpty && !isMobSound) return

        val loc = toCenterBlockLocation(block.location)
        val particleColor = note.toDouble() / 24.0

        if (!isMobSound) {
            block.world.playSound(loc, sound!!, 1.0f, pitch)
            block.world.spawnParticle(Particle.NOTE, loc.add(0.0, 1.2, 0.0), 0, particleColor, 0.0, 0.0, 1.0)
        } else block.world.playSound(loc, sound!!, 1.0f, 1.0f)

        block.world.sendGameEvent(player, GameEvent.NOTE_BLOCK_PLAY, loc.toVector())
    }

    fun increaseNote() {
        container.set(noteKey, PersistentDataType.BYTE, ((note + 1) % 25).toByte())

        block.updateSurroundingBlocks()
    }

    fun setPowered(powered: Boolean) {
        if (powered) {
            if (!isPowered) block.updateSurroundingBlocks()
            container.set(poweredKey, PersistentDataType.BOOLEAN, true)
        }
        else {
            if (isPowered) block.updateSurroundingBlocks()
            container.remove(poweredKey)
        }
    }

    fun removeData() {
        container.remove(noteKey)
        container.remove(poweredKey)
    }

    val isMobSound: Boolean
        get() = when (blockAbove.type) {
            Material.SKELETON_SKULL, Material.ZOMBIE_HEAD, Material.PIGLIN_HEAD, Material.CREEPER_HEAD, Material.DRAGON_HEAD, Material.WITHER_SKELETON_SKULL, Material.PLAYER_HEAD -> true
            else -> false
        }

    private fun Block.updateSurroundingBlocks() {
        val oldData = blockData
        type = Material.BARRIER
        setBlockData(oldData, false)
    }
}
