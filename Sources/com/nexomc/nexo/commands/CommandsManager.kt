package com.nexomc.nexo.commands

import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.utils.logs.Logs
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import java.net.URI
import java.util.*

object CommandsManager {
    fun loadCommands() {
        commandTree("nexo", namespace = "nexo") {
            withAliases("n", "nx")
            withPermission("nexo.command")
            packCommand()
            updateCommand()
            recipesCommand()
            debugCommand()
            itemInfoCommand()
            blockInfoCommand()
            glyphInfoCommand()
            versionCommand()
            reloadCommand()
            emojiCommand()
            takeItemCommand()
            itemInfoCommand()
            inventoryCommand()
            giveItemCommand()
            dumpLogCommand()
            convertCommand()
            dyeCommand()
            literalArgument("test") {
                playerExecutor { player, commandArguments ->
                    Logs.debug("test", "d" as Any)
                    player.sendResourcePacks(ResourcePackRequest.addingRequest(ResourcePackInfo.resourcePackInfo(
                        UUID.randomUUID(), URI.create("http://atlas.oraxen.com:8080/pack.zip?id=53bdb371078286f40a45cdc5f455ae37ed42df95"), "53bdb371078286f40a45cdc5f455ae37ed42df95"
                    )
                    ).replace(true) )
                }
            }

            anyExecutor { sender, _ ->
                Message.COMMAND_HELP.send(sender)
            }
        }
    }
}
