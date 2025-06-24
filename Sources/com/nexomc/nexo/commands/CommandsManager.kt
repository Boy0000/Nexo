package com.nexomc.nexo.commands

import com.nexomc.nexo.configs.Message
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.commandTree

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
            dropItemCommand()
            giveItemCommand()
            dumpLogCommand()
            dyeCommand()

            anyExecutor { sender, _ ->
                Message.COMMAND_HELP.send(sender)
            }
        }
    }
}
