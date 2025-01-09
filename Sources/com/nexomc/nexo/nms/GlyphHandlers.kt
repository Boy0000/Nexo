package com.nexomc.nexo.nms

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.fonts.ShiftTag
import com.nexomc.nexo.utils.*
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.entity.Player
import team.unnamed.creative.font.Font
import java.util.Locale
import java.util.regex.Pattern

object GlyphHandlers {

    private val randomKey = Key.key("random")
    private val randomComponent = NexoPlugin.instance().fontManager().glyphFromID("required")!!.glyphComponent()
    private val defaultEmoteReplacementConfigs = NexoPlugin.instance().fontManager().glyphs().filter { it.font == Font.MINECRAFT_DEFAULT }
        .associateFast { it to TextReplacementConfig.builder().match(it.character()).replacement(Component.textOfChildren(randomComponent)).build() }

    private val colorableRegex = Pattern.compile("<glyph:.*:(c|colorable)>")

    fun escapePlaceholders(component: Component, player: Player?): Component {
        var component = component

        NexoPlugin.instance().fontManager().glyphs().forEach { glyph ->
            val config = if (glyph.hasPermission(player)) glyph.placeholderReplacementConfig else return@forEach
            component = component.replaceText(config ?: return@forEach)
        }

        return component
    }

    fun escapeGlyphs(component: Component, player: Player?): Component {
        var component = component
        val serialized = component.asFlatTextContent()

        // Replace all unicodes found in default font with a random one
        // This is to prevent use of unicodes from the font the chat is in
        defaultEmoteReplacementConfigs.filterFast { !it.key.hasPermission(player) }.forEach { (_, config) ->
            component = component.replaceText(config)
        }

        // Replace raw unicode usage of non-permitted Glyphs with random font
        // This will always show a white square
        NexoPlugin.instance().fontManager().glyphs().forEach { glyph ->
            if (glyph.hasPermission(player)) return@forEach

            component = component.replaceText(glyph.escapeReplacementConfig)
        }

        return component.replaceText(ShiftTag.ESCAPE_REPLACEMENT_CONFIG)
    }

    fun unescapeGlyphs(component: Component): Component {
        var component = component

        NexoPlugin.instance().fontManager().glyphs().forEach { glyph ->
            component = component.replaceText(glyph.unescapeReplacementConfig)
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

        NexoPlugin.instance().fontManager().glyphs().filterFast { it.glyphTag() in serialized }.forEach { glyph ->
            component = component.replaceText(glyph.replacementConfig)
        }
        return component.replaceText(ShiftTag.REPLACEMENT_CONFIG)
    }
}
