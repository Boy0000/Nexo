package com.nexomc.nexo.mechanics.combat.spell.thor

import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.combat.spell.SpellMechanic
import org.bukkit.Location
import org.bukkit.configuration.ConfigurationSection
import kotlin.random.Random

class ThorMechanic(factory: ThorMechanicFactory, section: ConfigurationSection) : SpellMechanic(factory, section) {
    val lightningBoltsAmount: Int = section.getInt("lightning_bolts_amount")
    private val randomLocationVariation: Double = section.getDouble("random_location_variation")

    fun randomizedLocation(location: Location): Location {
        location.x = location.x + (Random.nextDouble() * randomLocationVariation) - randomLocationVariation / 2
        location.y = location.y + (Random.nextDouble() * randomLocationVariation) - randomLocationVariation / 2
        return location
    }
}
