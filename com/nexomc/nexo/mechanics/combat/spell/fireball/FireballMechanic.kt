package com.nexomc.nexo.mechanics.combat.spell.fireball

import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.combat.spell.SpellMechanic
import org.bukkit.configuration.ConfigurationSection

class FireballMechanic(factory: MechanicFactory, section: ConfigurationSection) : SpellMechanic(factory, section) {
    val yield: Double = section.getDouble("yield")
    val speed: Double = section.getDouble("speed")
}
