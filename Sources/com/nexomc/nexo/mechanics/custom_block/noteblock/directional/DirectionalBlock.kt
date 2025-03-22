package com.nexomc.nexo.mechanics.custom_block.noteblock.directional

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteBlockMechanic
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteBlockMechanicFactory
import net.kyori.adventure.key.Key
import org.bukkit.block.BlockFace
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

class DirectionalBlock(directionalSection: ConfigurationSection) {
    val parentBlock: String? = directionalSection.getString("parent_block")
    val directionalType: DirectionalType = DirectionalType.entries.firstOrNull { it.name == directionalSection.getString("directional_type", "LOG")!! } ?: DirectionalType.LOG

    // LOG type
    val yBlock: String? = directionalSection.getString("y_block")
    val xBlock: String? = directionalSection.getString("x_block")
    val zBlock: String? = directionalSection.getString("z_block")

    // FURNACE/DROPPER type
    val northBlock: String? = directionalSection.getString("north_block")
    val southBlock: String? = directionalSection.getString("south_block")
    val eastBlock: String? = directionalSection.getString("east_block")
    val westBlock: String? = directionalSection.getString("west_block")

    // DROPPER type only
    val upBlock: String? = directionalSection.getString("up_block")
    val downBlock: String? = directionalSection.getString("down_block")

    fun isParentBlock() = parentBlock == null

    val parentMechanic = NoteBlockMechanicFactory.instance()?.getMechanic(parentBlock)

    val isLog = directionalType == DirectionalType.LOG
    val isFurnace = directionalType == DirectionalType.FURNACE
    val isDropper = directionalType == DirectionalType.DROPPER

    enum class DirectionalType {
        LOG, FURNACE, DROPPER
    }

    fun directionMechanic(face: BlockFace) = directionMechanic(face, null)
    fun directionMechanic(face: BlockFace, player: Player?) = NexoBlocks.noteBlockMechanic(when {
        isLog -> when (face) {
            BlockFace.NORTH, BlockFace.SOUTH -> xBlock
            BlockFace.EAST, BlockFace.WEST -> zBlock
            BlockFace.UP, BlockFace.DOWN -> yBlock
            else -> null
        }

        else -> when (player?.let(::relativeFacing) ?: face) {
            BlockFace.NORTH -> northBlock
            BlockFace.SOUTH -> southBlock
            BlockFace.EAST -> eastBlock
            BlockFace.WEST -> westBlock
            BlockFace.UP -> upBlock
            BlockFace.DOWN -> downBlock
            else -> null
        }
    })

    private fun relativeFacing(player: Player): BlockFace {
        val (yaw, pitch) = player.location.let { it.yaw.toDouble() to it.pitch.toDouble() }
        val face = when {
            isLog -> return BlockFace.SELF
            yaw in 0.0..45.0 || yaw >= 315.0 || yaw >= -45.0 && yaw <= 0.0 || yaw <= -315.0 -> BlockFace.NORTH
            yaw in 45.0..135.0 || yaw in -315.0..-225.0 -> BlockFace.EAST
            yaw in 135.0..225.0 || yaw in -225.0..-135.0 -> BlockFace.SOUTH
            yaw in 225.0..315.0 || yaw in -135.0..-45.0 -> BlockFace.WEST
            else -> BlockFace.SELF
        }

        return when {
            !isDropper -> face
            pitch <= -45.0 -> BlockFace.DOWN
            pitch >= 45.0 -> BlockFace.UP
            else -> face
        }
    }

    fun directionalModel(mechanic: NoteBlockMechanic) = Key.key(
        mechanic.section.getString("model")
            ?: mechanic.directional?.parentMechanic?.model?.asString()
            ?: mechanic.section.parent!!.getString("Pack.model", mechanic.itemID)
            ?: mechanic.itemID
    )

    fun anyMatch(itemId: String?) = when {
        xBlock == itemId -> true
        zBlock == itemId -> true
        yBlock == itemId -> true
        upBlock == itemId -> true
        downBlock == itemId -> true
        westBlock == itemId -> true
        eastBlock == itemId -> true
        northBlock == itemId -> true
        southBlock == itemId -> true
        else -> false
    }
}
