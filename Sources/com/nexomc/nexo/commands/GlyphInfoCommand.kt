package com.nexomc.nexo.commands

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.utils.deserialize
import com.nexomc.nexo.utils.toTypedArray
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.stringArgument
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import java.util.concurrent.CompletableFuture

internal fun CommandTree.glyphInfoCommand() = literalArgument("glyphinfo") {
    withPermission("nexo.command.glyphinfo")
    stringArgument("glyphid") {
        replaceSuggestions(ArgumentSuggestions.stringsAsync {
            CompletableFuture.supplyAsync { NexoPlugin.instance().fontManager().glyphs().toTypedArray { it.id } }
        })
        anyExecutor { sender, args ->
            val glyphId = args.get("glyphid") as? String ?: return@anyExecutor
            val glyph = NexoPlugin.instance().fontManager().glyphFromID(glyphId)
                ?: return@anyExecutor sender.sendMessage("<red>No glyph found with glyph-id <i><dark_red>$glyphId".deserialize())

            sender.sendMessage("<dark_aqua>GlyphID: <aqua>$glyphId".deserialize())
            sender.sendMessage("<dark_aqua>Texture: <aqua>${glyph.texture.asString()}".deserialize())
            sender.sendMessage("<dark_aqua>Font: <aqua>${glyph.font.asString()}".deserialize())
            sender.sendMessage(
                Component.text("Unicode:", NamedTextColor.DARK_AQUA).append(glyph.component
                .hoverEvent(HoverEvent.showText(Component.text("Click to copy to clipboard!", NamedTextColor.GOLD)))
                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, glyph.unicodes.joinToString("")))
            ))
        }
    }
}
