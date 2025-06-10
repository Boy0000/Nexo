package com.nexomc.nexo.utils.commands

import com.nexomc.nexo.utils.AdventureUtils.parseMiniMessage
import com.nexomc.nexo.utils.withOp
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

class CommandsParser(section: ConfigurationSection, tagResolver: TagResolver?) {
    private var consoleCommands: List<String> = section.getStringList("console").map { parseMiniMessage(it, tagResolver) }
    private var playerCommands: List<String> = section.getStringList("player")
    private var oppedPlayerCommands: List<String> = section.getStringList("opped_player")

    fun perform(player: Player) {
        val playerName = player.name

        consoleCommands.forEach { command ->
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%p%", playerName))
        }

        playerCommands.forEach { command ->
            player.performCommand(command.replace("%p", playerName))
        }

        player.withOp {
            oppedPlayerCommands.forEach { command ->
                player.performCommand(command.replace("%p%", playerName))
            }
        }
    }
}
