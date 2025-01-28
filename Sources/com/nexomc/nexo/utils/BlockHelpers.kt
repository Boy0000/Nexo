package com.nexomc.nexo.utils

import com.jeff_media.customblockdata.CustomBlockData
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager
import io.papermc.paper.math.Position
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.type.Snow
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.plugin.java.JavaPlugin
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

        return blockFaces.mapFast(blockBelow::getRelative)
            .firstOrNull { (it.type != Material.AIR || IFurniturePacketManager.blockIsHitbox(block)) && it.boundingBox.overlaps(entityBox) }
    }
    private val blockFaces = BlockFace.entries.take(4)

    @JvmStatic
    fun playCustomBlockSound(location: Location, sound: String?, volume: Float, pitch: Float) {
        playCustomBlockSound(toCenterLocation(location), sound, SoundCategory.BLOCKS, volume, pitch)
    }

    @JvmStatic
    fun playCustomBlockSound(location: Location?, sound: String?, category: SoundCategory?, volume: Float, pitch: Float) {
        if (sound == null || location == null || location.world == null || category == null) return
        location.world.playSound(location, validateReplacedSounds(sound)!!, category, volume, pitch)
    }

    @JvmStatic
    fun validateReplacedSounds(sound: String?): String? {
        val sound = sound?.removePrefix("minecraft:") ?: return null
        val mechanics = NexoPlugin.instance().configsManager().mechanics

        return when {
            sound.startsWith("block.wood") && mechanics.getBoolean("noteblock.custom_block_sounds") -> sound.prependIfMissing("nexo:")
            sound.startsWith("block.stone") && mechanics.getBoolean("stringblock.custom_block_sounds") -> sound.prependIfMissing("nexo:")
            else -> sound
        }
    }

    @JvmStatic
    fun toBlockLocation(location: Location): Location {
        return location.clone().also {
            it.x = location.blockX.toDouble()
            it.y = location.blockY.toDouble()
            it.z = location.blockZ.toDouble()
        }
    }

    @JvmStatic
    fun toCenterLocation(location: Location): Location {
        return location.clone().also {
            it.x = location.blockX + 0.5
            it.y = location.blockY + 0.5
            it.z = location.blockZ + 0.5
        }
    }

    @JvmStatic
    fun toCenterBlockLocation(location: Location): Location {
        return toCenterLocation(location).subtract(0.0, 0.5, 0.0)
    }

    @JvmStatic
    fun updateSurroundingBlocks(block: Block) {
        val oldData = block.blockData
        block.type = Material.BARRIER
        block.setBlockData(oldData, false)
    }

    @JvmStatic
    fun isStandingInside(player: Player?, block: Block?): Boolean {
        if (player == null || block == null) return false
        // Since the block might be AIR, Block#getBoundingBox returns an empty one
        // Get the block-center and expand it 0.5 to cover the block
        val blockBox = BoundingBox.of(toCenterLocation(block.location), 0.5, 0.5, 0.5)

        return block.world.getNearbyEntities(blockBox).any { it is LivingEntity && (it !is Player || it.gameMode != GameMode.SPECTATOR) }
    }

    /** Returns the PersistentDataContainer from CustomBlockData
     * @param block The block to get the PersistentDataContainer for
     */
    val Block.persistentDataContainer: PersistentDataContainer get() = CustomBlockData(this, NexoPlugin.instance())

    /** Returns the PersistentDataContainer from CustomBlockData
     * @param block The block to get the PersistentDataContainer for
     * @param plugin The plugin to get the CustomBlockData from
     */
    fun getPersistentDataContainer(block: Block, plugin: JavaPlugin): PersistentDataContainer {
        return CustomBlockData(block, plugin)
    }

    @JvmField
    val REPLACEABLE_BLOCKS: ObjectOpenHashSet<Material> = ObjectOpenHashSet(Tag.REPLACEABLE.values)

    @JvmStatic
    fun isReplaceable(block: Block, excludeUUID: UUID? = null): Boolean {
        return when (block.type) {
            Material.SNOW -> (block.blockData as Snow).layers == 1
            in REPLACEABLE_BLOCKS -> true
            Material.SCULK_VEIN -> true
            else -> false
        } && !IFurniturePacketManager.blockIsHitbox(block, excludeUUID)
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
     * Also checks if the block is an Nexo block or not as NoteBlocks are Interacable
     */
    @JvmStatic
    fun isInteractable(placedAgainst: Block?): Boolean {
        if (placedAgainst == null) return false

        val noteBlockMechanic = NexoBlocks.noteBlockMechanic(placedAgainst)
        val furnitureMechanic = NexoFurniture.furnitureMechanic(placedAgainst.location)
        val type = placedAgainst.type

        return when {
            noteBlockMechanic != null -> false
            furnitureMechanic != null -> furnitureMechanic.isInteractable
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

    /*
     * Calling loc.getChunk() will crash a Paper 1.19 build 62-66 (possibly more) Server if the Chunk does not exist.
     * Instead, get Chunk location and check with World.isChunkLoaded() if the Chunk is loaded.
     */
    @JvmStatic
    fun isLoaded(world: World, loc: Location): Boolean {
        return world.isChunkLoaded(loc.blockX shr 4, loc.blockZ shr 4)
    }

    @JvmStatic
    fun isLoaded(loc: Location): Boolean {
        return loc.world != null && isLoaded(loc.world, loc)
    }
}
