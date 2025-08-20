package com.nexomc.nexo.commands

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.utils.childSections
import com.nexomc.nexo.utils.remove
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.literalArgument

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
            literalArgument("reset_custom_model_data") {
                anyExecutor { _ ,_ ->
                    NexoPlugin.instance().configsManager().itemConfigs.forEach { key, value ->
                        value.childSections().forEach { it.value.remove("Pack.custom_model_data") }
                        value.save(key)
                    }
                }
            }

            anyExecutor { sender, _ ->
                Message.COMMAND_HELP.send(sender)
            }
        }
    }
}
