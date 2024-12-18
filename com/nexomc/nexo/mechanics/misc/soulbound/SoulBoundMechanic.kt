package com.nexomc.nexo.mechanics.misc.soulbound

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.mechanics.Mechanic
import com.nexomc.nexo.mechanics.MechanicFactory
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection

class SoulBoundMechanic(mechanicFactory: MechanicFactory?, section: ConfigurationSection) :
    Mechanic(mechanicFactory, section) {
    val loseChance = section.getDouble("lose_chance")

    companion object {
        val NAMESPACED_KEY = NamespacedKey(NexoPlugin.instance(), "soulbound")
    }
}
