package com.nexomc.nexo.nms

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.glyphs.Glyph
import com.nexomc.nexo.glyphs.ShiftTag
import com.nexomc.nexo.utils.associateFastWith
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.entity.Player
import team.unnamed.creative.font.Font
import java.util.*

object GlyphHandlers {

    private val randomComponent by lazy {
        Component.textOfChildren(NexoPlugin.instance().fontManager().glyphFromID("required")!!.glyphComponent())
    }
    private val defaultEmoteReplacementConfigs by lazy {
        NexoPlugin.instance().fontManager().glyphs().filter { it.font == Font.MINECRAFT_DEFAULT }.associateFastWith {
            when (it.unicodes.size) {
                1 -> TextReplacementConfig.builder().matchLiteral(it.unicodes.first())
                else -> TextReplacementConfig.builder().match("(${it.unicodes.joinToString("|").removeSuffix("|")})")
            }.replacement(randomComponent).build()
        }
    }

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
        defaultEmoteReplacementConfigs.forEach { (glyph, config) ->
            if (!glyph.hasPermission(player) && glyph.chars.any(serialized::contains))
                component = component.replaceText(config)
        }

        // Replace raw unicode usage of non-permitted Glyphs with random font
        // This will always show a white square
        NexoPlugin.instance().fontManager().glyphs().forEach { glyph ->
            if (!glyph.hasPermission(player)) return@forEach
            component = component.replaceText(glyph.escapeTagConfig)
        }

        if (ShiftTag.containsTag(serialized)) component = component.replaceText(ShiftTag.ESCAPE_REPLACEMENT_CONFIG)

        return component
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
        val serialized = component.asFlatTextContent()

        if (Glyph.containsTagOrPlaceholder(serialized)) NexoPlugin.instance().fontManager().glyphs().forEach { glyph ->
            component = component.replaceText(glyph.unescapeTagConfig)
        }

        if (ShiftTag.containsTag(serialized)) component = component.replaceText(ShiftTag.ESCAPE_REPLACEMENT_CONFIG)

        return component
    }

    private fun Component.asFlatTextContent(): String {
        val flatText = (this@asFlatTextContent as? TextComponent) ?: return ""
        return buildString {
            append(flatText.content())
            append(flatText.children().joinToString("") { it.asFlatTextContent() })
            (flatText.hoverEvent()?.value() as? Component)?.let { hover ->
                val hoverText = hover as? TextComponent ?: return@let
                append(hoverText.content())
                append(hoverText.children().joinToString("") { it.asFlatTextContent() })
            }
        }
    }

    @JvmStatic
    fun Component.transformGlyphs(locale: Locale? = null): Component {
        var component = GlobalTranslator.render(this, locale ?: Locale.US)
        val serialized = component.asFlatTextContent()

        if (Glyph.containsTagOrPlaceholder(serialized)) NexoPlugin.instance().fontManager().glyphs().forEach { glyph ->
            if (glyph.baseRegex.containsMatchIn(serialized)) component = component.replaceText(glyph.tagConfig)
            if (glyph.placeholders.any(serialized::contains)) glyph.placeholderConfig?.let { component = component.replaceText(it) }
        }

        if (ShiftTag.containsTag(serialized)) component = component.replaceText(ShiftTag.REPLACEMENT_CONFIG)

        return component
    }
}
