package com.nexomc.nexo.mechanics.light

import com.nexomc.nexo.mechanics.furniture.FurnitureFactory
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.utils.VersionUtil
import net.kyori.adventure.key.Key
import org.bukkit.Location
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.ItemDisplay

class LightMechanic(
    val lightBlocks: MutableList<LightBlock>,
    val toggleable: Boolean = false,
    val toggledModel: String? = null,
    val toggledItemModel: Key? = null
) {

    constructor(section: ConfigurationSection) : this(
        section.getStringList("lights.lights").map(::LightBlock).toMutableList(),
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
    }
}
