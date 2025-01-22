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

            anyExecutor { sender, _ ->
                Message.COMMAND_HELP.send(sender)
            }
        }
    }
}
