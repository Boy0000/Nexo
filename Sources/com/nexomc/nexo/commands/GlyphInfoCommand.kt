package com.nexomc.nexo.commands

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.fonts.Glyph
import com.nexomc.nexo.utils.AdventureUtils
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.stringArgument
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import java.util.concurrent.CompletableFuture

internal fun CommandTree.glyphInfoCommand() = literalArgument("glyphinfo") {
    withPermission("nexo.command.glyphinfo")
    stringArgument("glyphid") {
        replaceSuggestions(ArgumentSuggestions.stringsAsync {
            CompletableFuture.supplyAsync { NexoPlugin.instance().fontManager().glyphs().map(Glyph::id).toTypedArray() }
        })
        anyExecutor { sender, args ->
            val mm = AdventureUtils.MINI_MESSAGE
            val glyphId = args.get("glyphid") as? String ?: return@anyExecutor
            val audience = NexoPlugin.instance().audience().sender(sender)
            val glyph = NexoPlugin.instance().fontManager().glyphFromID(glyphId)
                ?: return@anyExecutor audience.sendMessage(mm.deserialize("<red>No glyph found with glyph-id <i><dark_red>$glyphId"))

            audience.sendMessage(mm.deserialize("<dark_aqua>GlyphID: <aqua>$glyphId"))
            audience.sendMessage(mm.deserialize("<dark_aqua>Texture: <aqua>${glyph.texture.asString()}"))
            audience.sendMessage(mm.deserialize("<dark_aqua>Font: <aqua>${glyph.font().asString()}"))
            audience.sendMessage(mm.deserialize("<dark_aqua>Bitmap: <aqua>${glyph.isBitMap}"))
            audience.sendMessage(mm.deserialize("<dark_aqua>Unicode: <white>${glyph.character()}")
                .hoverEvent(HoverEvent.showText(mm.deserialize("<gold>Click to copy to clipboard!")))
                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, glyph.character())))
        }
    }
}
