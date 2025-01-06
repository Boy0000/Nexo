package com.nexomc.nexo.commands

import com.mineinabyss.idofront.items.asColorable
import com.mineinabyss.idofront.items.editItemMeta
import com.mineinabyss.idofront.util.ColorHelpers
import com.mineinabyss.idofront.util.removeSpaces
import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.utils.mapNotNullFast
import com.nexomc.nexo.utils.safeCast
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.greedyStringArgument
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Color
import org.bukkit.Material

internal fun CommandTree.dyeCommand() = literalArgument("dye") {
    withPermission("nexo.command.dye")
    greedyStringArgument("color") {
        /*replaceSuggestions(ArgumentSuggestions.stringsAsync {
            CompletableFuture.supplyAsync {
                when {
                    it.currentInput.startsWith("#") -> arrayOf("#FEFEFE", "#123456")
                    it.currentInput.contains(",") -> arrayOf("255,255,255")
                    else -> NamedTextColor.NAMES.keys().filter { n -> it.currentInput.isEmpty() || n.startsWith(it.currentInput) }.toTypedArray()
                }
            }
        })*/
        playerExecutor { player, args ->
            val color = args.get("color").safeCast<String>()?.toColor() ?: return@playerExecutor Message.DYE_WRONG_COLOR.send(player)
            val item = player.inventory.itemInMainHand.takeIf { it.type != Material.AIR }
                ?: player.inventory.itemInOffHand.takeIf { it.type != Material.AIR }
                ?: return@playerExecutor Message.DYE_FAILED.send(player)
            item.editItemMeta {
                asColorable()?.apply {
                    this.color = color
                } ?: return@playerExecutor Message.DYE_FAILED.send(player)
            }
            Message.DYE_SUCCESS.send(player)
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun String.toColor(): Color? {
    return runCatching {
        when {
            this.startsWith("#") -> Color.fromARGB(this.drop(1).padStart(8, 'F').hexToInt(ColorHelpers.hexFormat))
            this.startsWith("0x") -> Color.fromARGB(this.drop(2).padStart(8, 'F').hexToInt(ColorHelpers.hexFormat))
            "," in this -> {
                val color = this.removeSpaces().split(",")
                when (color.mapNotNullFast(String::toIntOrNull).size) {
                    3 -> Color.fromRGB(color[0].toInt(), color[1].toInt(), color[2].toInt())
                    4 -> Color.fromARGB(color[0].toInt(), color[1].toInt(), color[2].toInt(), color[3].toInt())
                    else -> null
                }
            }
            else -> NamedTextColor.NAMES.value(this)?.value()?.let(Color::fromRGB)
        }
    }.getOrNull()
}