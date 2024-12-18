package com.nexomc.nexo.fonts

import com.nexomc.nexo.NexoPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.entity.Player

object GlyphTag {
    private const val GLYPH = "glyph"
    private const val GLYPH_SHORT = "g"
    val RESOLVER = TagResolver.resolver(mutableSetOf(GLYPH, GLYPH_SHORT)) { args, ctx -> glyphTag(null, args) }

    fun getResolverForPlayer(player: Player?): TagResolver {
        return TagResolver.resolver(setOf(GLYPH, GLYPH_SHORT)) { args, ctx -> glyphTag(player, args) }
    }

    fun glyphTag(player: Player?, args: ArgumentQueue): Tag {
        val glyphId = args.popOr("A glyph value is required").value()
        val glyph = NexoPlugin.instance().fontManager().glyphFromName(glyphId)
        val colorable = args.hasNext() && (args.peek()!!.value() == "colorable" || args.peek()!!.value() == "c")
        var glyphComponent = Component.text(glyph.character()).style(Style.empty()).font(glyph.font())

        glyphComponent = when {
            glyph.hasPermission(player) -> glyphComponent.color(NamedTextColor.WHITE.takeUnless { colorable })
            else -> Component.text(glyph.glyphTag())
        }
        return Tag.selfClosingInserting(glyphComponent)
    }
}
