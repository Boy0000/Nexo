package com.nexomc.nexo.mechanics.furniture.evolution

import org.bukkit.configuration.ConfigurationSection
import kotlin.random.Random

class EvolvingFurniture(
    val delay: Int,
    var minimumLightLevel: Int = 0,
    var lightBoostTick: Int = 0,
    var rainBoostTick: Int,
    var boneMealChance: Int = 0,
    val nextStage: String?,
    private val probability: Int,
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

    fun bernoulliTest() = Random.nextInt(probability) == 0
}
