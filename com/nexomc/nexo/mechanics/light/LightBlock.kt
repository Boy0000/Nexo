package com.nexomc.nexo.mechanics.light

import com.nexomc.nexo.mechanics.furniture.BlockLocation
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.data.type.Light

class LightBlock : BlockLocation {
    private val lightLevel: Int
    private val lightData: Light

    fun from(hitboxObject: Any?): LightBlock {
        return when (hitboxObject) {
            is String -> LightBlock(hitboxObject)
            else -> LightBlock("0,0,0 15")
        }
    }

    constructor(hitboxString: String) : super(hitboxString.substringBefore(" ")) {
        this.lightLevel = hitboxString.substringAfter(" ").toIntOrNull() ?: 15
        this.lightData = Material.LIGHT.createBlockData { (it as Light).level = lightLevel } as Light
    }

    constructor(location: Location, lightData: Light) : super(location) {
        this.lightLevel = lightData.level
        this.lightData = lightData
    }

    fun lightLevel(): Int {
        return lightLevel
    }

    fun lightData(): Light {
        return lightData
    }
}
