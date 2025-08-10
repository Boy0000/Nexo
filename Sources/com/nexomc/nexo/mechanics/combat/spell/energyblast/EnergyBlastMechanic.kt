package com.nexomc.nexo.mechanics.combat.spell.energyblast

import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.combat.spell.SpellMechanic
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.configuration.ConfigurationSection

class EnergyBlastMechanic(factory: EnergyBlastMechanicFactory, section: ConfigurationSection) : SpellMechanic(factory, section) {
    val particle: Particle
    var particleColor: Particle.DustOptions? = null
    val damage: Double = section.getDouble("damage")
    val length: Int = section.getInt("length")

    init {
        val particleSection = checkNotNull(section.getConfigurationSection("particle"))
        this.particle = Particle.valueOf(particleSection.getString("type")!!)
        particleSection.getConfigurationSection("color")?.let { colorSection ->
            this.particleColor = Particle.DustOptions(
                Color.fromRGB(colorSection.getInt("red"), colorSection.getInt("green"), colorSection.getInt("blue")),
                particleSection.getInt("size").toFloat()
            )
        }
    }
}
