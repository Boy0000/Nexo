package com.nexomc.nexo.utils

import com.jeff_media.customblockdata.CustomBlockData
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager
import io.papermc.paper.math.Position
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.SoundCategory
import org.bukkit.Tag
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.type.Snow
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.util.BoundingBox
import java.util.*

object BlockHelpers {
    /**
     * Returns the block the entity is standing on.<br></br>
     * Mainly to handle cases where player is on the edge of a block, with AIR below them
     */
    @JvmStatic
    fun entityStandingOn(entity: Entity): Block? {
        val (block, blockBelow) = entity.location.block.let { it to it.getRelative(BlockFace.DOWN) }
        if (!block.type.isAir && block.type != Material.LIGHT) return block
        if (!blockBelow.type.isAir) return blockBelow

        val entityBox = entity.boundingBox.expand(0.3)

        return blockFaces.firstNotNullOfOrNull { blockFace ->
           blockBelow.getRelative(blockFace).takeIf { it.boundingBox.overlaps(entityBox) && (it.type != Material.AIR || IFurniturePacketManager.blockIsHitbox(block)) }
        }
    }
    private val blockFaces = ObjectOpenHashSet.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)

    @JvmStatic
    fun playCustomBlockSound(location: Location, sound: String?, volume: Float, pitch: Float) {
        playCustomBlockSound(location.toCenterLocation(), sound, SoundCategory.BLOCKS, volume, pitch)
    }

    @JvmStatic
    fun playCustomBlockSound(location: Location?, sound: String?, category: SoundCategory?, volume: Float, pitch: Float) {
        if (sound == null || category == null || location == null || !location.isLoaded) return
        location.world.playSound(location, sound, category, volume, pitch)
    }

    @JvmStatic
    fun toCenterBlockLocation(location: Location): Location {
        return location.toCenterLocation().subtract(0.0, 0.5, 0.0)
    }

    @JvmStatic
    fun isStandingInside(player: Player?, block: Block?): Boolean {
        if (player == null || block == null) return false
        // Since the block might be AIR, Block#getBoundingBox returns an empty one
        // Get the block-center and expand it 0.5 to cover the block
        val blockBox = BoundingBox.of(block.location.toCenterLocation(), 0.5, 0.5, 0.5)

        return block.world.getNearbyEntities(blockBox).any { it is LivingEntity && (it !is Player || it.gameMode != GameMode.SPECTATOR) }
    }

    val Block.persistentDataContainer: PersistentDataContainer get() = CustomBlockData(this, NexoPlugin.instance())

    @JvmField
    val REPLACEABLE_BLOCKS: ObjectOpenHashSet<Material> = ObjectOpenHashSet(Tag.REPLACEABLE.values)

    @JvmStatic
    fun isReplaceable(block: Block, excludeUUID: UUID? = null): Boolean {
        return when (block.type) {
            Material.SNOW -> (block.blockData as Snow).layers == 1
            Material.SCULK_VEIN -> true
            in REPLACEABLE_BLOCKS -> true
            else -> false
        } && !IFurniturePacketManager.blockIsHitbox(block, excludeUUID, collisionOnly = false)
    }

    @JvmStatic
    fun isReplaceable(position: Position, world: World, excludeUUID: UUID? = null): Boolean {
        val block = position.toLocation(world).block
        return isReplaceable(block, excludeUUID)
    }

    @JvmStatic
    fun isReplaceable(material: Material) = material in REPLACEABLE_BLOCKS || material == Material.SCULK_VEIN

    /**
     * Improved version of [Material.isInteractable] intended for replicating vanilla behavior.
     * Checks if the block one places against is interactable in the sense a chest is
     * Also checks if the block is a Nexo block or not as NoteBlocks are Interacable
     */
    @JvmStatic
    fun isInteractable(placedAgainst: Block?, player: Player? = null): Boolean {
        if (placedAgainst == null) return false

        val noteBlockMechanic = NexoBlocks.noteBlockMechanic(placedAgainst)
        val furnitureMechanic = NexoFurniture.furnitureMechanic(placedAgainst.location)
        val type = placedAgainst.type

        return when {
            noteBlockMechanic != null -> false
            furnitureMechanic != null -> furnitureMechanic.isInteractable(player)
            Tag.STAIRS.isTagged(type) -> false
            Tag.FENCES.isTagged(type) -> false
            Tag.DOORS.isTagged(type) -> false
            !type.isInteractable -> false
            else -> when (type) {
                Material.PUMPKIN, Material.MOVING_PISTON, Material.REDSTONE_ORE, Material.REDSTONE_WIRE, Material.IRON_TRAPDOOR -> false
                else -> true
            }
        }
    }

    val Location.isLoaded get() = this.isWorldLoaded && this.isChunkLoaded
}
