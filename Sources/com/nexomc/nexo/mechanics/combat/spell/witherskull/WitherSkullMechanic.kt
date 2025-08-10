package com.nexomc.nexo.mechanics.combat.spell.witherskull

import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.combat.spell.SpellMechanic
import com.nexomc.nexo.utils.timers.TimersFactory
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

class WitherSkullMechanic(factory: WitherSkullMechanicFactory, section: ConfigurationSection) : SpellMechanic(factory, section) {
    private val timersFactory: TimersFactory = TimersFactory(section.getLong("delay"))
    val charged: Boolean = section.getBoolean("charged")

    override fun timer(player: Player) = timersFactory.getTimer(player)
}
