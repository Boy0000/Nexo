package com.nexomc.nexo.mechanics.custom_block.stringblock.sapling

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.compatibilities.worldedit.WrappedWorldEdit
import com.nexomc.nexo.configs.ConfigsManager
import com.nexomc.nexo.utils.appendIfMissing
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.configuration.ConfigurationSection
import java.io.File
import kotlin.random.Random

class SaplingMechanic(itemId: String?, section: ConfigurationSection) {
    val canGrowNaturally: Boolean = section.getBoolean("grows_naturally", true)
    val naturalGrowthTime: Int = section.getInt("natural_growth_time", 6000)
    val canGrowFromBoneMeal: Boolean = section.getBoolean("grows_from_bonemeal", true)
    val boneMealGrowthSpeedup: Int = section.getInt("bonemeal_growth_speedup", naturalGrowthTime / 5).coerceAtMost(naturalGrowthTime)
    val growSound: String? = section.getString("grow_sound", null)
    val minLightLevel: Int = section.getInt("min_light_level", 0)
    val requiresWaterSource: Boolean = section.getBoolean("requires_water_source", false)
    private val schematicConfig: Any? = section.get("schematic")
    val copyBiomes: Boolean = section.getBoolean("copy_biomes", false)
    val copyEntities: Boolean = section.getBoolean("copy_entities", false)
    val replaceBlocks: Boolean = section.getBoolean("replace_blocks", false)


    fun canPlaceSchematic(location: Location, schematic: File): Boolean {
        return replaceBlocks || WrappedWorldEdit.blocksInSchematic(location, schematic, true).isEmpty()
    }

    fun placeSchematic(location: Location, schematic: File) {
        location.block.setType(Material.AIR, false)
        if (growSound != null) location.world.playSound(location, growSound, 1.0f, 0.8f)
        WrappedWorldEdit.pasteSchematic(location, schematic, replaceBlocks, copyBiomes, copyEntities)
    }

    fun requiresLight(): Boolean {
        return minLightLevel != 0
    }

    fun hasGrowSound(): Boolean {
        return growSound != null
    }

    fun isUnderWater(block: Block): Boolean {
        return block.getRelative(BlockFace.DOWN).type == Material.WATER
    }

    fun selectSchematic(): File? {
        val schematics = schematics()
        if (schematics.isEmpty()) return null

        val totalWeight = schematics.sumOf { it.chance }
        val randomWeight = Random.nextDouble(totalWeight)

        var cumulativeWeight = 0.0
        for (schematic in schematics) {
            cumulativeWeight += schematic.chance
            if (randomWeight <= cumulativeWeight) {
                return getSchematicFile(schematic.name)
            }
        }
        return null
    }

    fun hasSchematic(): Boolean {
        return schematics().isNotEmpty() && schematic() != null
    }

    fun schematics(): List<SchematicEntry> {
        return when (schematicConfig) {
            is String -> listOf(SchematicEntry(schematicConfig.appendIfMissing(".schem"), 1.0))
            is List<*> -> schematicConfig.mapNotNull {
                (it as? Map<*, *>)?.let { map ->
                    val schemName = map["schem"] as? String
                    val chance = map["chance"] as? Double ?: 1.0
                    if (schemName != null) SchematicEntry(
                        schemName.appendIfMissing(".schem"),
                        chance
                    ) else null
                }
            }

            else -> emptyList()
        }
    }

    fun getSchematicFile(schematicName: String): File? {
        val schem = File(ConfigsManager.schematicsFolder.absolutePath + "/" + schematicName)
        return if (!schem.exists()) null else schem
    }

    fun schematicFiles(): List<File> {
        return schematics().mapNotNull { getSchematicFile(it.name) }
    }

    fun schematic(): File? {
        return schematics().firstOrNull()?.let { getSchematicFile(it.name) }
    }

    companion object {
        val SAPLING_KEY = NamespacedKey(NexoPlugin.instance(), "sapling")
    }

    data class SchematicEntry(val name: String, val chance: Double)
}
