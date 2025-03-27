package com.nexomc.nexo.commands

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.fonts.Glyph
import com.nexomc.nexo.utils.AdventureUtils
import com.nexomc.nexo.utils.mapFast
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.stringArgument
import java.util.concurrent.CompletableFuture
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent

internal fun CommandTree.glyphInfoCommand() = literalArgument("glyphinfo") {
    withPermission("nexo.command.glyphinfo")
    stringArgument("glyphid") {
        replaceSuggestions(ArgumentSuggestions.stringsAsync {
            CompletableFuture.supplyAsync { NexoPlugin.instance().fontManager().glyphs().mapFast(Glyph::id).toTypedArray() }
        })
        anyExecutor { sender, args ->
            val mm = AdventureUtils.MINI_MESSAGE
            val glyphId = args.get("glyphid") as? String ?: return@anyExecutor
            val glyph = NexoPlugin.instance().fontManager().glyphFromID(glyphId)
                ?: return@anyExecutor sender.sendMessage(mm.deserialize("<red>No glyph found with glyph-id <i><dark_red>$glyphId"))

            sender.sendMessage(mm.deserialize("<dark_aqua>GlyphID: <aqua>$glyphId"))
            sender.sendMessage(mm.deserialize("<dark_aqua>Texture: <aqua>${glyph.texture.asString()}"))
            sender.sendMessage(mm.deserialize("<dark_aqua>Font: <aqua>${glyph.font.asString()}"))
            sender.sendMessage(mm.deserialize("<dark_aqua>Unicode: <white>${glyph.unicodes.joinToString("\n")}")
                .hoverEvent(HoverEvent.showText(mm.deserialize("<gold>Click to copy to clipboard!")))
                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, glyph.unicodes.joinToString())))
        }
    }
}
