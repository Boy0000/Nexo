package com.nexomc.nexo.commands

import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.utils.ensureCast
import dev.jorel.commandapi.kotlindsl.*
import org.bukkit.block.BlockFace
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
            literalArgument("admin") {
                stringArgument("furnitureid") {
                    integerArgument("radius") {
                        playerExecutor { player, args ->
                            val furniture = args.get("furnitureid").ensureCast<String>().let(NexoFurniture::furnitureMechanic) ?: return@playerExecutor
                            val radius = args.get("radius").ensureCast<Int>()

                            val center = player.location.toBlockLocation().add(10.0, 0.0, 0.0)
                            for (x in -radius..radius) {
                                for (y in -radius..radius) {
                                    for (z in -radius..radius) {
                                        val loc = center.clone().add(x.toDouble(), y.toDouble(), z.toDouble())
                                        furniture.place(loc, 0f, BlockFace.UP, false)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            anyExecutor { sender, _ ->
                Message.COMMAND_HELP.send(sender)
            }
        }
    }
}
