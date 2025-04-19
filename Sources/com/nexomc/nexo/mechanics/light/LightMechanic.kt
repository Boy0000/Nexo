package com.nexomc.nexo.mechanics.light

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.items.ItemBuilder
import com.nexomc.nexo.mechanics.furniture.FurnitureFactory
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.getStringListOrNull
import com.nexomc.nexo.utils.toIntRangeOrNull
import net.kyori.adventure.key.Key
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.ItemDisplay

class LightMechanic(
    val lightBlocks: MutableList<LightBlock>,
    val toggleable: Boolean = false,
    private val _toggledModel: String? = null,
    private val _toggledItemModel: Key? = null
) {

    val toggledModel by lazy { NexoItems.itemFromId(_toggledModel) }
    val toggledItemModel by lazy { _toggledItemModel?.let { ItemBuilder(Material.LEATHER_HORSE_ARMOR).setItemModel(it) } }

    constructor(section: ConfigurationSection) : this(
        (section.getStringListOrNull("lights.lights") ?: section.getString("lights.lights")?.let(::listOf) ?: listOf()).flatMap(::parseLights).toMutableList(),
        section.getBoolean("lights.toggleable"),
        section.getString("lights.toggled_model"),
        section.getString("lights.toggled_item_model").takeIf { VersionUtil.atleast("1.21.3") }?.let(Key::key)
    )

    val isEmpty: Boolean
        get() = lightBlocks.all { it.lightLevel == 0 }

    fun lightBlockLocations(center: Location, rotation: Float): List<Location> {
        return lightBlocks.map { it.groundRotate(rotation).add(center) }
    }

    fun refreshLights(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {
        val packetManager = FurnitureFactory.instance()?.packetManager() ?: return
        packetManager.removeLightMechanicPacket(baseEntity, mechanic)
        packetManager.sendLightMechanicPacket(baseEntity, mechanic)
    }

    companion object {
        private val EMPTY = LightMechanic(mutableListOf())

        fun parseLights(hitboxString: String): List<LightBlock> {
            return when {
                hitboxString == "origin" -> listOf(LightBlock("0,0,0"))
                ".." in hitboxString -> {
                    // Split the coordinates by commas
                    val coordinates = hitboxString.split(",").map { r -> r.substringBefore(" ").toIntRangeOrNull() ?: (r.substringBefore("").toIntOrNull() ?: 0).let { IntRange(it, it) } }

                    val xRange = coordinates[0]
                    val yRange = coordinates[1]
                    val zRange = coordinates[2]
                    val lightLevel = hitboxString.substringAfter(" ").toIntOrNull()?.coerceIn(1, 15) ?: 15

                    // Generate combinations of all the ranges
                    mutableListOf<LightBlock>().apply {
                        for (x in xRange) for (y in yRange) for (z in zRange) {
                            this += LightBlock(x, y, z, lightLevel)
                        }
                    }
                }
                else -> listOf(LightBlock(hitboxString))
            }
        }
    }
}
