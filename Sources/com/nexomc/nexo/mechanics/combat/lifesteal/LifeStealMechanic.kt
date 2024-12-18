package com.nexomc.nexo.mechanics.combat.lifesteal

import com.nexomc.nexo.mechanics.Mechanic
import com.nexomc.nexo.mechanics.MechanicFactory
import org.bukkit.configuration.ConfigurationSection

class LifeStealMechanic(mechanicFactory: MechanicFactory, section: ConfigurationSection) : Mechanic(mechanicFactory, section) {
    val amount: Int = section.getInt("amount")
}
