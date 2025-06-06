package com.nexomc.nexo.commands

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.deserialize
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import net.kyori.adventure.inventory.Book
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent

internal fun CommandTree.emojiCommand() = literalArgument("emojis") {
    withPermission("nexo.command.emojis")
    playerExecutor { player, _ ->
        val onlyShowPermissable = Settings.SHOW_PERMISSION_EMOJIS.toBool()
        val emojis = NexoPlugin.instance().fontManager().emojis().filter { !onlyShowPermissable || it.hasPermission(player) }
            .takeUnless { it.isEmpty() } ?: return@playerExecutor Message.NO_EMOJIS.send(player)
        val emojiComponents = emojis.map { emoji ->
            val hoverText = emoji.placeholders.joinToString("\n").plus(when {
                onlyShowPermissable -> ""
                emoji.hasPermission(player) -> "\n<green>Permitted"
                else -> "\n<red>No Permission"
            }).deserialize()

            val hoverEvent = HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText)
            emoji.glyphComponent().hoverEvent(hoverEvent)
        }

        val bookPages = emojiComponents.chunked(255) { Component.textOfChildren(*it.toTypedArray()) }
        val book = Book.book(Component.text("Glyph Book"), Component.text("Nexo"), bookPages)
        player.openBook(book)
    }
}
