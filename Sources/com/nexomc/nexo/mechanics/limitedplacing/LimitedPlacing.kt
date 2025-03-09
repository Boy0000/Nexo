package com.nexomc.nexo.mechanics.limitedplacing

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.utils.BlockHelpers
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.configuration.ConfigurationSection

class LimitedPlacing(section: ConfigurationSection) {
    val type: LimitedPlacingType = LimitedPlacingType.entries.firstOrNull { section.getString("type", "DENY") == it.name } ?: LimitedPlacingType.DENY
    private val blockTypes: List<Material> = limitedBlockMaterials(section.getStringList("block_types"))
    private val blockTags: Set<Tag<Material>> = limitedBlockTags(section.getStringList("block_tags"))
    private val nexoBlocks: List<String> = limitedNexoBlocks(section.getStringList("nexo_blocks"))
    val radiusLimitation: RadiusLimitation? = section.getConfigurationSection("radius_limitation")?.let(::RadiusLimitation)
    val isFloor = section.getBoolean("floor", true)
    val isRoof = section.getBoolean("roof", true)
    val isWall = section.getBoolean("wall", true)

    class RadiusLimitation(section: ConfigurationSection) {
        val radius = section.getInt("radius", -1)
        val amount = section.getInt("amount", -1)
    }

    val isRadiusLimited: Boolean
        get() = radiusLimitation != null && radiusLimitation.radius != -1 && radiusLimitation.amount != -1

    private fun limitedBlockMaterials(list: List<String>) = list.mapNotNull(Material::getMaterial)

    private fun limitedNexoBlocks(list: List<String>): List<String> {
        return list.filter { e -> NexoBlocks.isCustomBlock(e) || NexoFurniture.isFurniture(e) }
    }

    private fun limitedBlockTags(list: List<String>) =
        list.mapNotNull { Bukkit.getTag(Tag.REGISTRY_BLOCKS, NamespacedKey.minecraft(it), Material::class.java) }.toSet()

    fun isNotPlacableOn(block: Block, blockFace: BlockFace): Boolean {
        val placedBlock = if (BlockHelpers.isReplaceable(block)) block else block.getRelative(blockFace)
        val blockBelow = placedBlock.getRelative(BlockFace.DOWN)
        val blockAbove = placedBlock.getRelative(BlockFace.UP)

        if (isWall && block.type.isSolid() && blockFace.modY == 0) return false
        if (isFloor && (blockFace == BlockFace.UP || blockBelow.type.isSolid())) return false
        if (isRoof && blockFace == BlockFace.DOWN) return false
        return !isRoof || !blockAbove.type.isSolid()
    }

    fun limitedBlocks(): List<Material> {
        return blockTypes
    }

    fun limitedNexoBlockIds(): List<String> {
        return nexoBlocks
    }

    fun limitedTags(): Set<Tag<Material>> {
        return blockTags
    }

    fun checkLimited(block: Block): Boolean {
        if (blockTypes.isEmpty() && blockTags.isEmpty() && nexoBlocks.isEmpty()) return type == LimitedPlacingType.ALLOW
        val nexoId = checkIfNexoItem(block) ?: return block.type in blockTypes || blockTags.any { it.isTagged(block.type) }

        return (nexoBlocks.isNotEmpty() && nexoId in nexoBlocks)
    }

    private fun checkIfNexoItem(block: Block) = (NexoBlocks.customBlockMechanic(block.blockData) ?: NexoFurniture.furnitureMechanic(block.location))?.itemID

    enum class LimitedPlacingType {
        ALLOW, DENY
    }
}
