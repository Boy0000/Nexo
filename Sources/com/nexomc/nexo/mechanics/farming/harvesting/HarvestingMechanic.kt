package com.nexomc.nexo.mechanics.farming.harvesting

import com.nexomc.nexo.mechanics.Mechanic
import com.nexomc.nexo.utils.timers.Timer
import com.nexomc.nexo.utils.timers.TimersFactory
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

class HarvestingMechanic(factory: HarvestingMechanicFactory, section: ConfigurationSection) : Mechanic(factory, section) {
    val radius: Int = section.getInt("radius")
    val height: Int = section.getInt("height")
    val lowerItemDurability: Boolean = section.getBoolean("lower_item_durability", true)
    private val timersFactory: TimersFactory = TimersFactory(section.getInt("cooldown").coerceAtLeast(0))

    fun getTimer(player: Player): Timer = timersFactory.getTimer(player)
}
