package com.nexomc.nexo.glyphs

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.commands.toColor
import com.nexomc.nexo.utils.toIntRangeOrNull
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.entity.Player

object GlyphTag {
    private const val GLYPH = "glyph"
    private const val GLYPH_SHORT = "g"
    val RESOLVER = TagResolver.resolver(mutableSetOf(GLYPH, GLYPH_SHORT)) { args, ctx -> glyphTag(null, args) }

    fun containsTag(string: String): Boolean {
        return string.contains("<$GLYPH:") || string.contains("<$GLYPH_SHORT:")
    }

    fun getResolverForPlayer(player: Player?): TagResolver {
        return TagResolver.resolver(setOf(GLYPH, GLYPH_SHORT)) { args, ctx -> glyphTag(player, args) }
    }

    fun glyphTag(player: Player?, args: ArgumentQueue): Tag {
        val glyphId = args.popOr("A glyph value is required").value()
        val glyph = NexoPlugin.instance().fontManager().glyphFromName(glyphId)
        val arguments = mutableListOf<String>()
        while (args.hasNext()) arguments.add(args.pop().value())

        val colorable = "colorable" in arguments || "c" in arguments
        val shadow = arguments.elementAtOrNull(arguments.indexOfFirst { it == "shadow" || it == "s" } + 1)
        val bitmapIndexRange = arguments.firstNotNullOfOrNull { it.toIntRangeOrNull() ?: it.toIntOrNull()?.let { IntRange(it, it) } } ?: IntRange.EMPTY
        val glyphComponent = when {
            glyph.hasPermission(player) -> glyph.glyphComponent(colorable, GlyphShadow(shadow?.toColor()), bitmapIndexRange)
            else -> Component.text(glyph.glyphTag)
        }
        return Tag.selfClosingInserting(glyphComponent)
    }
}
