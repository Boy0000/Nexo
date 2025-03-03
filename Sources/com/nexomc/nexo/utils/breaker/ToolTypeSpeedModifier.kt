package com.nexomc.nexo.utils.breaker

import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic
import org.bukkit.Material
import org.bukkit.Tag

class ToolTypeSpeedModifier {
    private val toolType: Set<Material>
    private val speedModifier: Float
    private val materials: Set<Material>
    private val customBlocks: Set<CustomBlockMechanic>

    constructor(toolType: Set<Material>, speedModifier: Float) {
        this.toolType = toolType
        this.speedModifier = speedModifier
        this.materials = HashSet()
        this.customBlocks = HashSet()
    }

    constructor(toolType: Set<Material>, speedModifier: Float, materials: Set<Material>) {
        this.toolType = toolType
        this.speedModifier = speedModifier
        this.materials = materials
        this.customBlocks = HashSet()
    }

    constructor(toolType: Set<Material>, speedModifier: Float, customBlocks: Collection<CustomBlockMechanic>) {
        this.toolType = toolType
        this.speedModifier = speedModifier
        this.materials = HashSet()
        this.customBlocks = HashSet(customBlocks)
    }

    fun toolTypes(): Set<Material> {
        return toolType
    }

    fun speedModifier(): Float {
        return speedModifier
    }

    fun materials(): Set<Material> {
        return materials
    }

    fun customBlocks(): Set<CustomBlockMechanic> {
        return customBlocks
    }

    companion object {
        @JvmField
        val EMPTY = ToolTypeSpeedModifier(mutableSetOf(Material.AIR), 1f)
        @JvmField
        val VANILLA: MutableSet<ToolTypeSpeedModifier> = mutableSetOf()

        init {
            VANILLA.add(EMPTY)

            val itemTools = mutableSetOf<Material>()
            itemTools += Tag.ITEMS_SHOVELS.values
            itemTools += Tag.ITEMS_SWORDS.values
            itemTools += Tag.ITEMS_AXES.values
            itemTools += Tag.ITEMS_PICKAXES.values
            itemTools += Tag.ITEMS_HOES.values

            VANILLA += ToolTypeSpeedModifier(itemTools.filter { it.name.startsWith("WOODEN_") }.toSet(), 2f)
            VANILLA += ToolTypeSpeedModifier(itemTools.filter { it.name.startsWith("STONE_") }.toSet(), 4f)
            VANILLA += ToolTypeSpeedModifier(itemTools.filter { it.name.startsWith("IRON_") }.toSet(), 6f)
            VANILLA += ToolTypeSpeedModifier(itemTools.filter { it.name.startsWith("DIAMOND_") }.toSet(), 8f)
            VANILLA += ToolTypeSpeedModifier(itemTools.filter { it.name.startsWith("NETHERITE_") }.toSet(), 9f)
            VANILLA += ToolTypeSpeedModifier(itemTools.filter { it.name.startsWith("GOLDEN_") }.toSet(), 12f)

            VANILLA += ToolTypeSpeedModifier(mutableSetOf(Material.SHEARS), 15f, Tag.LEAVES.values)
            VANILLA += ToolTypeSpeedModifier(mutableSetOf(Material.SHEARS), 15f, mutableSetOf(Material.COBWEB))
            VANILLA += ToolTypeSpeedModifier(mutableSetOf(Material.SHEARS), 5f, Tag.WOOL.values)
            VANILLA += ToolTypeSpeedModifier(mutableSetOf(Material.SHEARS), 2f)

            VANILLA += ToolTypeSpeedModifier(Tag.ITEMS_SWORDS.values, 15f, Tag.SWORD_EFFICIENT.values)
            VANILLA += ToolTypeSpeedModifier(Tag.ITEMS_SWORDS.values, 1.5f)
        }
    }
}
