package com.nexomc.nexo.commands

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.glyphs.Glyph
import com.nexomc.nexo.utils.deserialize
import com.nexomc.nexo.utils.mapFast
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
            CompletableFuture.supplyAsync { NexoPlugin.instance().fontManager().glyphs().mapFast(Glyph::id).toTypedArray() }
        })
        anyExecutor { sender, args ->
            val glyphId = args.get("glyphid") as? String ?: return@anyExecutor
            val glyph = NexoPlugin.instance().fontManager().glyphFromID(glyphId)
                ?: return@anyExecutor sender.sendMessage("<red>No glyph found with glyph-id <i><dark_red>$glyphId".deserialize())

            sender.sendMessage("<dark_aqua>GlyphID: <aqua>$glyphId".deserialize())
            sender.sendMessage("<dark_aqua>Texture: <aqua>${glyph.texture.asString()}".deserialize())
            sender.sendMessage("<dark_aqua>Font: <aqua>${glyph.font.asString()}".deserialize())
            sender.sendMessage("<dark_aqua>Unicode: <white><font:${glyph.font}>${glyph.formattedUnicodes}</font>".deserialize()
                .hoverEvent(HoverEvent.showText("<gold>Click to copy to clipboard!".deserialize()))
                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, glyph.unicodes.joinToString())))
        }
    }
}
