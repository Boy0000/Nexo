package com.nexomc.nexo.mechanics.misc.soulbound

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.mechanics.Mechanic
import com.nexomc.nexo.mechanics.MechanicFactory
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection

class SoulBoundMechanic(factory: SoulBoundMechanicFactory, section: ConfigurationSection) : Mechanic(factory, section) {
    val loseChance = section.getDouble("lose_chance")

    companion object {
        val NAMESPACED_KEY = NamespacedKey(NexoPlugin.instance(), "soulbound")
    }
}
