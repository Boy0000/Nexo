package com.nexomc.nexo.mechanics.light

import org.bukkit.Location
import org.bukkit.configuration.ConfigurationSection

class LightMechanic(val lightBlocks: List<LightBlock>) {

    constructor(section: ConfigurationSection) : this(section.getStringList("lights").map(::LightBlock))

    val isEmpty: Boolean
        get() = lightBlocks.all { it.lightLevel() == 0 }

    fun lightBlockLocations(center: Location, rotation: Float): List<Location> {
        return lightBlocks.map { it.groundRotate(rotation).add(center) }
    }

    companion object {
        private val EMPTY = LightMechanic(listOf())
    }
}
