package com.nexomc.nexo.commands

import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.utils.*
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.greedyStringArgument
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Color
import org.bukkit.inventory.ItemStack
import kotlin.jvm.optionals.getOrNull

internal fun CommandTree.dyeCommand() = literalArgument("dye") {
    withPermission("nexo.command.dye")
    greedyStringArgument("color", optional = true) {
        playerExecutor { player, args ->
            val color = args.getOptional("color").getOrNull()?.safeCast<String>()?.let { it.toColor() ?: return@playerExecutor Message.DYE_WRONG_COLOR.send(player) }
            val item = player.inventory.itemInMainHand.takeUnless(ItemStack::isEmpty)
                ?: player.inventory.itemInOffHand.takeUnless(ItemStack::isEmpty)
                ?: return@playerExecutor Message.DYE_FAILED.send(player)
            item.asColorable()?.apply {
                this.color = color
                Message.DYE_SUCCESS.send(player)
            } ?: return@playerExecutor Message.DYE_FAILED.send(player)
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
private val hexFormat = HexFormat {
    this.upperCase = true
    this.number {
        this.removeLeadingZeros = false
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun String.toColor(): Color? {
    return runCatching {
        when {
            startsWith("#") || startsWith("0x") -> {
                val hex = removePrefix("#").removePrefix("0x")
                Color.fromARGB(hex.padStart(8, 'F').take(8).hexToInt(hexFormat))
            }
            "," in this -> {
                val color = removeSpaces().split(",")
                when (color.mapNotNullFast(String::toIntOrNull).size) {
                    3 -> Color.fromRGB(color[0].toInt(), color[1].toInt(), color[2].toInt())
                    4 -> Color.fromARGB(color[0].toInt(), color[1].toInt(), color[2].toInt(), color[3].toInt())
                    else -> null
                }
            }
            this.toIntOrNull() != null -> Color.fromRGB(this.toInt())
            else -> NamedTextColor.NAMES.value(this)?.value()?.let(Color::fromRGB)
        }
    }.printOnFailure(true).getOrNull()
}
