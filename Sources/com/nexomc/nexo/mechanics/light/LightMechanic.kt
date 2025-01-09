package com.nexomc.nexo.mechanics.light

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.items.ItemBuilder
import com.nexomc.nexo.mechanics.furniture.FurnitureHelpers
import net.kyori.adventure.key.Key
import org.bukkit.Location
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.ItemDisplay

class LightMechanic(
    val lightBlocks: MutableList<LightBlock>,
    val toggleable: Boolean = false,
    val lightModel: String? = null
) {

    constructor(section: ConfigurationSection) : this(
        section.getStringList("lights").map(::LightBlock).toMutableList(),
        section.getBoolean("lights_toggleable"),
        section.getString("lights_model")
    )

    val isEmpty: Boolean
        get() = lightBlocks.all { it.lightLevel == 0 }

    fun lightBlockLocations(center: Location, rotation: Float): List<Location> {
        return lightBlocks.map { it.groundRotate(rotation).add(center) }
    }

    fun lightModelItem(baseEntity: ItemDisplay): ItemBuilder? {
        return lightModel?.takeIf { toggleable && FurnitureHelpers.lightState(baseEntity) }?.let(NexoItems::itemFromId)
    }

    companion object {
        private val EMPTY = LightMechanic(mutableListOf())
    }
}
