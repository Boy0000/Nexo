package com.nexomc.nexo.mechanics.farming.smelting

import com.nexomc.nexo.mechanics.Mechanic
import com.nexomc.nexo.mechanics.MechanicFactory
import org.bukkit.configuration.ConfigurationSection

class SmeltingMechanic(mechanicFactory: MechanicFactory?, section: ConfigurationSection) :
    Mechanic(mechanicFactory, section) {
    private val playSound: Boolean = section.getBoolean("play_sound")

    fun playSound(): Boolean {
        return this.playSound
    }
}