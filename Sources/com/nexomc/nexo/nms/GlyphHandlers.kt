package com.nexomc.nexo.nms

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.fonts.ShiftTag
import com.nexomc.nexo.utils.AdventureUtils
import com.nexomc.nexo.utils.logs.Logs
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.translation.GlobalTranslator
import org.apache.commons.lang3.StringUtils
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import java.util.Locale
import java.util.regex.Pattern

object GlyphHandlers {
    @JvmField
    val GLYPH_HANDLER_KEY = NamespacedKey(NexoPlugin.instance(), "glyph_handler")

    private val randomKey = Key.key("random")
    private val colorableRegex = Pattern.compile("<glyph:.*:(c|colorable)>")

    private fun escapeGlyphs(component: Component, player: Player): Component {
        var component = GlobalTranslator.render(component, player.locale())
        val serialized = AdventureUtils.MINI_MESSAGE.serialize(component)

        // Replace raw unicode usage of non-permitted Glyphs with random font
        // This will always show a white square
        NexoPlugin.instance().fontManager().glyphs().forEach { glyph ->
            if (glyph.hasPermission(player)) return@forEach

            component = component.replaceText(
                TextReplacementConfig.builder()
                    .matchLiteral(glyph.character())
                    .replacement(glyph.glyphComponent().font(randomKey))
                    .build()
            )

            // Escape all glyph-tags
            val matcher = glyph.baseRegex.matcher(serialized)
            while (matcher.find()) {
                component = component.replaceText(
                    TextReplacementConfig.builder().once()
                        .matchLiteral(matcher.group())
                        .replacement(AdventureUtils.MINI_MESSAGE.deserialize("\\" + matcher.group()))
                        .build()
                )
            }
        }

        return component
    }

    fun unescapeGlyphs(component: Component): Component {
        var component = component
        val serialized = component.asFlatTextContent()

        NexoPlugin.instance().fontManager().glyphs().asSequence().map { it.escapedRegex.matcher(serialized) }.forEach {
                while (it.find()) {
                    component = component.replaceText(
                        TextReplacementConfig.builder().once()
                            .matchLiteral(it.group())
                            .replacement(
                                AdventureUtils.STANDARD_MINI_MESSAGE.deserialize(
                                    StringUtils.removeStart(
                                        it.group(),
                                        "\\"
                                    )
                                )
                            )
                            .build()
                    )
                }
            }

        return component
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
        NexoPlugin.instance().fontManager().glyphs().forEach { glyph ->
            component = component.replaceText(glyph.replacementConfig)
        }
        return component.replaceText(ShiftTag.REPLACEMENT_CONFIG)
    }
}
