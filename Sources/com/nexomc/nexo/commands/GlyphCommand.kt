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
        val emojiList = NexoPlugin.instance().fontManager().emojis()
        val onlyShowPermissable = Settings.SHOW_PERMISSION_EMOJIS.toBool()

        val emojis = if (onlyShowPermissable) emojiList else emojiList.filter { it.hasPermission(player) }
        val pages = mutableListOf<Component>()
        var size: Int

        if (emojis.isEmpty()) return@playerExecutor Message.NO_EMOJIS.send(player)

        pageLoop@ for (page in 0..49) {
            for (i in 0..255) {
                size = page * 256 + i + 1
                if (emojis.size < size) break@pageLoop
                val emoji = emojis[page * 256 + i]
                val hoverText = emoji.placeholders.joinToString("\n").plus(when {
                    onlyShowPermissable -> ""
                    emoji.hasPermission(player) -> "\n<green>Permitted"
                    else -> "\n<red>No Permission"
                }).deserialize()

                val hoverEvent = HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText)
                pages += "<glyph:${emoji.id}>".deserialize().hoverEvent(hoverEvent)
            }
        }

        val book = Book.book(Component.text("Glyph Book"), Component.text("Nexo"), pages)
        player.openBook(book)
    }
}
