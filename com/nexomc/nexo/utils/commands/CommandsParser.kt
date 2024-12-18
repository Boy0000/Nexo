package com.nexomc.nexo.utils.commands

import com.nexomc.nexo.utils.AdventureUtils.parseMiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

class CommandsParser(section: ConfigurationSection?, tagResolver: TagResolver?) {
    private var consoleCommands: List<String>? = null
    private var playerCommands: List<String>? = null
    private var oppedPlayerCommands: List<String>? = null
    private var empty = false

    init {
        if (section == null) empty = true
        else {
            if (section.isList("console"))
                this.consoleCommands = section.getStringList("console").map { parseMiniMessage(it, tagResolver) }

            if (section.isList("player")) this.playerCommands = section.getStringList("player")

            if (section.isList("opped_player")) this.oppedPlayerCommands = section.getStringList("opped_player")
        }
    }

    fun perform(player: Player) {
        if (empty) return
        val playerName = player.name

        if (consoleCommands != null) for (command in consoleCommands!!) Bukkit.dispatchCommand(
            Bukkit.getConsoleSender(),
            command.replace("%p%", playerName)
        )

        if (playerCommands != null) for (command in playerCommands!!) Bukkit.dispatchCommand(
            player,
            command.replace("%p%", playerName)
        )

        if (oppedPlayerCommands != null) for (command in oppedPlayerCommands!!) {
            val wasOp = player.isOp
            player.isOp = true
            Bukkit.dispatchCommand(player, command.replace("%p%", playerName))
            player.isOp = wasOp
        }
    }
}
