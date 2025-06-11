package com.nexomc.nexo.nms

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.commands.toColor
import com.nexomc.nexo.glyphs.GlyphShadow
import com.nexomc.nexo.glyphs.Shift
import com.nexomc.nexo.glyphs.ShiftTag
import com.nexomc.nexo.utils.associateFastWith
import com.nexomc.nexo.utils.filterFast
import com.nexomc.nexo.utils.serialize
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.entity.Player
import team.unnamed.creative.font.Font
import java.util.*

object GlyphHandlers {

    private val randomComponent = NexoPlugin.instance().fontManager().glyphFromID("required")!!.glyphComponent()
    private val defaultEmoteReplacementConfigs = NexoPlugin.instance().fontManager().glyphs().filter { it.font == Font.MINECRAFT_DEFAULT }
        .associateFastWith {
            TextReplacementConfig.builder().match("(${it.unicodes.joinToString("|")})")
                .replacement(Component.textOfChildren(randomComponent)).build()
        }

    val shiftRegex: Regex = "(?<!\\\\)<shift:(-?\\d+)>".toRegex()
    val escapedShiftRegex: Regex = "\\\\<shift:(-?\\d+)>".toRegex()
    private val colorableRegex: Regex = "\\|(c|colorable)".toRegex()
    private val glyphShadowRegex = "(?:shadow|s):(\\S+)".toRegex()
    private val bitmapIndexRegex: Regex = "\\|([0-9]+)(?:\\.\\.([0-9]+))?:".toRegex()

    fun escapeGlyphs(component: Component, player: Player?): Component {
        return escapePlaceholders(escapeGlyphTags(component, player), player)
    }

    fun escapePlaceholders(component: Component, player: Player?): Component {
        var component = component

        NexoPlugin.instance().fontManager().glyphs().forEach { glyph ->
            val config = glyph.escapePlaceholderConfig?.takeUnless { glyph.hasPermission(player) } ?: return@forEach
            component = component.replaceText(config)
        }

        return component
    }

    fun escapeGlyphTags(component: Component, player: Player?): Component {
        var component = component
        val serialized = component.asFlatTextContent()

        // Replace all unicodes found in default font with a random one
        // This is to prevent use of unicodes from the font the chat is in
        defaultEmoteReplacementConfigs.filterFast { !it.key.hasPermission(player) }.forEach { (glyph, config) ->
            if (glyph.unicodes.joinToString("").any(serialized::contains)) component = component.replaceText(config)
        }

        // Replace raw unicode usage of non-permitted Glyphs with random font
        // This will always show a white square
        NexoPlugin.instance().fontManager().glyphs().forEach { glyph ->
            val config = glyph.escapeTagConfig.takeUnless { glyph.hasPermission(player) } ?: return@forEach
            component = component.replaceText(config)
        }

        return component.replaceText(ShiftTag.ESCAPE_REPLACEMENT_CONFIG)
    }

    fun unescapeGlyphs(component: Component): Component {
        return unescapePlaceholders(unescapeGlyphTags(component))
    }

    fun unescapePlaceholders(component: Component): Component {
        var component = component

        NexoPlugin.instance().fontManager().glyphs().forEach { glyph ->
            component = component.replaceText(glyph.unescapePlaceholderConfig ?: return@forEach)
        }

        return component
    }

    fun unescapeGlyphTags(component: Component): Component {
        var component = component

        NexoPlugin.instance().fontManager().glyphs().forEach { glyph ->
            component = component.replaceText(glyph.unescapeTagConfig)
        }

        return component.replaceText(ShiftTag.ESCAPE_REPLACEMENT_CONFIG)
    }

    private fun Component.asFlatTextContent(): String {
        var flattened = ""
        val flatText = (this@asFlatTextContent as? TextComponent) ?: return flattened
        flattened += flatText.content()
        flattened += flatText.children().joinToString("") { it.asFlatTextContent() }
        (flatText.hoverEvent()?.value() as? Component)?.let { hover ->
            val hoverText = hover as? TextComponent ?: return@let
            flattened += hoverText.content()
            flattened += hoverText.children().joinToString("") { it.asFlatTextContent() }
        }

        return flattened
    }

    @JvmStatic
    fun Component.transformGlyphs(locale: Locale? = null): Component {
        var component = GlobalTranslator.render(this, locale ?: Locale.US)
        val serialized = component.asFlatTextContent()

        NexoPlugin.instance().fontManager().glyphs().filterFast {
            it.placeholders.any(serialized::contains) || it.baseRegex in serialized
        }.forEach { glyph ->
            component = component.replaceText(glyph.tagConfig)
            component = component.replaceText(glyph.placeholderConfig ?: return@forEach)
        }
        return component.replaceText(ShiftTag.REPLACEMENT_CONFIG)
    }

    @JvmStatic
    fun String.transformGlyphs(): String {
        var content = this

        for (glyph in NexoPlugin.instance().fontManager().glyphs()) glyph.baseRegex.findAll(this).forEach { match ->
            val colorable = colorableRegex in match.value
            val shadow = GlyphShadow(glyphShadowRegex.find(match.value)?.groupValues?.firstOrNull()?.toColor())
            val bitmapMatch = bitmapIndexRegex.find(match.value)
            val startIndex = bitmapMatch?.groupValues?.get(1)?.toIntOrNull() ?: -1
            val endIndex = bitmapMatch?.groupValues?.get(2)?.toIntOrNull()?.coerceAtLeast(startIndex) ?: startIndex

            val component = glyph.glyphComponent(colorable, shadow, startIndex..endIndex).serialize()
            content = content.replaceFirst(glyph.baseRegex, component)
        }

        shiftRegex.findAll(this).forEach { match ->
            val shift = match.groupValues[1].toIntOrNull() ?: return@forEach
            val shiftRegex = "(?<!\\\\):space_(-?$shift+):".toRegex()

            content = content.replaceFirst(shiftRegex, Shift.of(shift))
        }

        return content
    }

    fun String.escapeGlyphTags(player: Player?): String {
        var content = this

        NexoPlugin.instance().fontManager().glyphs().forEach {
            if (it.font != Font.MINECRAFT_DEFAULT || it.hasPermission(player)) return@forEach

            it.unicodes.forEach { unicode ->
                content = content.replace(unicode, "<font:random>$unicode</font>")
                unicode.forEach { char ->
                    content = content.replace(char.toString(), "<font:random>$unicode</font>")
                }
            }
        }

        for (glyph in NexoPlugin.instance().fontManager().glyphs()) glyph.baseRegex.findAll(this).forEach { match ->
            if (glyph.hasPermission(player)) return@forEach

            content = content.replaceFirst("(?<!\\\\)${match.value}", "\\${match.value}")
        }

        shiftRegex.findAll(this).forEach { match ->
            if (player?.hasPermission("nexo.shift") != false) return@forEach
            val space = match.groupValues[1].toIntOrNull() ?: return@forEach

            content = content.replaceFirst("(?<!\\\\)${match.value}", "\\<shift:$space>")
        }

        return content
    }

    fun String.unescapeGlyphTags(): String {
        var content = this

        for (glyph in NexoPlugin.instance().fontManager().glyphs()) glyph.escapedRegex.findAll(this).forEach { match ->
            content = content.replaceFirst(glyph.escapedRegex, match.value.removePrefix("\\"))
        }

        escapedShiftRegex.findAll(this).forEach { match ->
            content = content.replaceFirst(match.value, match.value.removePrefix("\\"))
        }

        return content
    }
}
