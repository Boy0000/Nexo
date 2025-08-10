package com.nexomc.nexo.mechanics.misc.commands

import com.nexomc.nexo.mechanics.Mechanic
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.utils.commands.CommandsParser
import com.nexomc.nexo.utils.timers.TimersFactory
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

class CommandsMechanic(factory: CommandsMechanicFactory, section: ConfigurationSection) : Mechanic(factory, section) {
    val commands = CommandsParser(section, null)
    private val timersFactory: TimersFactory = TimersFactory(section.getLong("cooldown"))
    var isOneUsage = section.getBoolean("one_usage")
    var permission: String? = section.getString("permission")

    fun hasPermission(player: Player) = permission == null || player.hasPermission(permission!!)

    fun getTimer(player: Player) = timersFactory.getTimer(player)
}
