package com.nexomc.nexo.mechanics.furniture.evolution

import org.bukkit.configuration.ConfigurationSection

class EvolvingFurniture(
    val delay: Int,
    var minimumLightLevel: Int = 0,
    var lightBoostTick: Int = 0,
    var rainBoostTick: Int,
    var boneMealChance: Int = 0,
    val nextStage: String?,
    val probability: Int,
) {

    constructor(plantSection: ConfigurationSection) : this(
        delay = plantSection.getInt("delay"),
        rainBoostTick = plantSection.getInt("rain_boost_tick"),
        nextStage = plantSection.getString("next_stage"),
        probability = (1.0 / plantSection.get("probability", 1) as Double).toInt()
    ) {
        plantSection.getConfigurationSection("light_boost")?.also {
            minimumLightLevel = it.getInt("minimum_light_level")
            lightBoostTick = it.getInt("boost_tick")
        }



        boneMealChance = plantSection.getInt("bone_meal_chance").coerceIn(0, 100)
    }
}
