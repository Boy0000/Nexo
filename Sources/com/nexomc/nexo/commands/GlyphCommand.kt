package com.nexomc.nexo.commands

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.AdventureUtils
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.literalArgument
import net.kyori.adventure.inventory.Book
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.entity.Player

internal fun CommandTree.emojiCommand() = literalArgument("emojis") {
    withPermission("nexo.command.emojis")
    anyExecutor { sender, _ ->
        val emojiList = NexoPlugin.instance().fontManager().emojis()
        val player = sender as? Player
        val onlyShowPermissable = Settings.SHOW_PERMISSION_EMOJIS.toBool()

        val emojis = emojiList.takeUnless { onlyShowPermissable } ?: emojiList.filter { it.hasPermission(player) }
        var pages = Component.empty()
        var size: Int

        if (emojis.isEmpty()) return@anyExecutor Message.NO_EMOJIS.send(player)

        pageLoop@ for (page in 0..49) {
            for (i in 0..255) {
                size = page * 256 + i + 1
                if (emojis.size < size) break@pageLoop
                val emoji = (emojis[page * 256 + i])
                val placeholders = emoji.placeholders
                var finalString = ""
                var permissionMessage = ""
                placeholders.forEach { placeholder ->
                    finalString += when {
                        placeholders.joinToString().endsWith(placeholder) -> placeholder
                        else -> (placeholder + "\n")
                    }

                    if (!onlyShowPermissable) permissionMessage += if (emoji.hasPermission(player)) ("\n<green>Permitted") else ("\n<red>No Permission")
                }

                pages = pages.append(
                    AdventureUtils.MINI_MESSAGE.deserialize("<glyph:" + emoji.id + ">")
                        .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, AdventureUtils.MINI_MESSAGE.deserialize(finalString + permissionMessage)))
                )
            }
        }

        val book = Book.book(Component.text("Glyph Book"), Component.text("Nexo"), pages)
        player?.openBook(book)
    }
}
