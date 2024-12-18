package com.nexomc.nexo.mechanics.custom_block.stringblock.sapling

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.ConfigsManager
import org.apache.commons.lang3.StringUtils
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.configuration.ConfigurationSection
import java.io.File

class SaplingMechanic(itemId: String?, section: ConfigurationSection) {
    val canGrowNaturally: Boolean = section.getBoolean("grows_naturally", true)
    val naturalGrowthTime: Int = section.getInt("natural_growth_time", 6000)
    val canGrowFromBoneMeal: Boolean = section.getBoolean("grows_from_bonemeal", true)
    val boneMealGrowthSpeedup: Int = section.getInt("bonemeal_growth_speedup", naturalGrowthTime / 5).coerceAtMost(naturalGrowthTime)
    val growSound: String? = section.getString("grow_sound", null)
    val minLightLevel: Int = section.getInt("min_light_level", 0)
    val requiresWaterSource: Boolean = section.getBoolean("requires_water_source", false)
    val schematicName: String? = StringUtils.appendIfMissing(section.getString("schematic", null), ".schem")
    val copyBiomes: Boolean = section.getBoolean("copy_biomes", false)
    val copyEntities: Boolean = section.getBoolean("copy_entities", false)
    val replaceBlocks: Boolean = section.getBoolean("replace_blocks", false)


    fun requiresLight(): Boolean {
        return minLightLevel != 0
    }

    fun hasGrowSound(): Boolean {
        return growSound != null
    }

    fun hasSchematic(): Boolean {
        return (schematicName != null && schematic() != null)
    }

    fun schematic(): File? {
        val schem = File(ConfigsManager.schematicsFolder.absolutePath + "/" + schematicName)
        return if (!schem.exists()) null else schem
    }

    fun isUnderWater(block: Block): Boolean {
        return block.getRelative(BlockFace.DOWN).type == Material.WATER
    }

    companion object {
        val SAPLING_KEY = NamespacedKey(NexoPlugin.instance(), "sapling")
    }
}
