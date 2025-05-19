package com.nexomc.nexo.commands

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.utils.safeCast
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.entitySelectorArgumentManyPlayers
import dev.jorel.commandapi.kotlindsl.literalArgument
import org.bukkit.entity.Player

internal fun CommandTree.packCommand() = literalArgument("pack") {
    withPermission("nexo.command.pack")
    entitySelectorArgumentManyPlayers("players", optional = true) {
        anyExecutor { sender, args ->
            val packServer = NexoPlugin.instance().packServer()
            args.getOptional("players").map { it.safeCast<List<Player>>() }.orElseGet {
                if (sender is Player) listOf(sender) else null
            }?.forEach(packServer::sendPack)
        }
    }
}
