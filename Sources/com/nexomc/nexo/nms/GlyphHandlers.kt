package com.nexomc.nexo.nms

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.glyphs.Glyph
import com.nexomc.nexo.glyphs.ShiftTag
import com.nexomc.nexo.utils.associateFastWith
import com.nexomc.nexo.utils.filterFast
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
        defaultEmoteReplacementConfigs.filterFast { !it.key.hasPermission(player) }.forEach { (glyph, config) ->
            if (glyph.chars.any(serialized::contains)) component = component.replaceText(config)
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
        val serialized = component.asFlatTextContent()

        if (Glyph.containsTagOrPlaceholder(serialized)) NexoPlugin.instance().fontManager().glyphs().forEach { glyph ->
            component = component.replaceText(glyph.unescapeTagConfig)
        }

        return component.replaceText(ShiftTag.ESCAPE_REPLACEMENT_CONFIG)
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

        if (Glyph.containsTagOrPlaceholder(serialized)) NexoPlugin.instance().fontManager().glyphs().filterFast {
            it.placeholders.any(serialized::contains) || it.baseRegex.containsMatchIn(serialized)
        }.forEach { glyph ->
            component = component.replaceText(glyph.tagConfig)
            component = component.replaceText(glyph.placeholderConfig ?: return@forEach)
        }
        return component.replaceText(ShiftTag.REPLACEMENT_CONFIG)
    }
}
