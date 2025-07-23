package com.nexomc.nexo.glyphs

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.commands.toColor
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.glyphs.GlyphShadow.Companion.glyphShadow
import com.nexomc.nexo.utils.*
import com.nexomc.nexo.utils.KeyUtils.appendSuffix
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import team.unnamed.creative.font.Font
import team.unnamed.creative.font.FontProvider
import java.util.regex.Pattern

open class Glyph(
    val id: String,
    open val font: Key,
    open val texture: Key,
    val ascent: Int,
    val height: Int,
    open val unicodes: List<String>,

    val permission: String = Settings.GLYPH_DEFAULT_PERMISSION.value?.toString()?.replace("<glyphid>", id) ?: "",
    val placeholders: List<String> = listOf(),
    val tabcomplete: Boolean = false,
    val isEmoji: Boolean = false,
) {

    open val defaultColor: TextColor = NamedTextColor.WHITE
    val chars by lazy { unicodes.flatMap { it.toList() }.toCharArray() }
    val formattedUnicodes by lazy { unicodes.joinToString("\n") { it.joinToString(Shift.of(-1)) } }
    val component by lazy { registerComponent() }
    open fun registerComponent(): Component {
        val placeholder = placeholders.firstOrNull()
        val hoverText = Settings.GLYPH_HOVER_TEXT.toString().let {
            if (placeholder != null) it.replace("<glyph_placeholder>", placeholder) else it
        }.replace("<glyph_id>", id).takeIf { it.isNotEmpty() }?.deserialize()?.let { HoverEvent.showText(it) }

        return Component.textOfChildren(*unicodes.flatMapIndexed { i, row ->
            val row = row.joinToString(Shift.of(-1))
            listOfNotNull(Component.text(row).font(font), Component.newline().takeIf { unicodes.size != i + 1 })
        }.toTypedArray()).hoverEvent(hoverText)
    }

    private fun bitmapComponent(bitmapIndex: Int, colorable: Boolean = false, shadow: GlyphShadow? = null, shift: String = Shift.of(0)) =
        Component.text("${chars.elementAtOrNull(bitmapIndex - 1) ?: chars.first()}$shift", if (colorable) null else defaultColor)
            .glyphShadow(shadow).font(font)
    private fun bitmapComponent(indexRange: IntRange, colorable: Boolean = false, shadow: GlyphShadow? = null) =
        Component.textOfChildren(*indexRange.map { bitmapComponent(it, colorable, shadow, Shift.of(-1).takeIf { indexRange.count() > 1 } ?: "") }.toTypedArray())

    private val _baseRegex = """<(glyph|g):($id(?::(?:c|colorable|\d+(?:\.\.\d+)?|s|shadow)(?::[\w#]+)?)*)>"""
    val baseRegex: Regex = Pattern.compile("(?<!\\\\)$_baseRegex").toRegex()
    val escapedRegex: Regex = Pattern.compile("\\\\" + _baseRegex).toRegex()
    private val placeholderMatch = "(${placeholders.joinToString("|", transform = Regex::escape)})"
    val tagConfig: TextReplacementConfig by lazy {
        TextReplacementConfig.builder().match(this.baseRegex.pattern).replacement { match, builder ->
            val args = match.group().substringAfter("<glyph:").substringAfter("<g:").substringBefore(">").split(":")
            val colorable = args.any { it == "colorable" || it == "c" }
            val shadow = args.elementAtOrNull(args.indexOfFirst { it == "shadow" || it == "s" } + 1)
            val bitmapIndex = args.firstNotNullOfOrNull { it.toIntRangeOrNull() ?: it.toIntOrNull()?.let { i ->IntRange(i, i) } } ?: IntRange.EMPTY
            glyphComponent(colorable, GlyphShadow(shadow?.toColor()), bitmapIndex)
        }.build()
    }
    val escapeTagConfig: TextReplacementConfig by lazy {
        TextReplacementConfig.builder().match(_baseRegex).replacement { match, b ->
            b.content("\\\\${match.group(1)}")
        }.build()
    }
    val unescapeTagConfig: TextReplacementConfig by lazy {
        TextReplacementConfig.builder().match(escapedRegex.pattern).replacement { match, b ->
            b.content(match.group(1).removePrefix("\\"))
        }.build()
    }
    val placeholderConfig: TextReplacementConfig? by lazy {
        TextReplacementConfig.builder().takeIf { placeholders.isNotEmpty() }
            ?.match("(?<!\\\\)$placeholderMatch")?.replacement(glyphComponent())?.build()
    }
    val escapePlaceholderConfig: TextReplacementConfig? by lazy {
        TextReplacementConfig.builder().takeIf { placeholders.isNotEmpty() }
            ?.match("(?<!\\\\)$placeholderMatch")?.replacement { match, b ->
                b.content("\\\\${match.group(1)}")
            }?.build()
    }
    val unescapePlaceholderConfig: TextReplacementConfig? by lazy {
        TextReplacementConfig.builder().takeIf { placeholders.isNotEmpty() }
            ?.match("\\\\$placeholderMatch")?.replacement { match, b ->
                b.content(match.group(1).removePrefix("\\"))
            }?.build()
    }

    constructor(id: String, font: Key, texture: Key, ascent: Int, height: Int, unicode: String) : this(id, font, texture, ascent, height, listOf(unicode))

    constructor(glyphSection: ConfigurationSection) : this(
        glyphSection.name,
        glyphSection.getKey("font", Settings.GLYPH_DEFAULT_FONT.toKey(Font.MINECRAFT_DEFAULT)),
        glyphSection.getKey("texture", REQUIRED_GLYPH).appendSuffix(".png"),
        glyphSection.getInt("ascent", 8),
        glyphSection.getInt("height", 8),
        calculateGlyphUnicodes(glyphSection),

        glyphSection.getString("permission", Settings.GLYPH_DEFAULT_PERMISSION.value?.toString()?.replace("<glyphid>", glyphSection.name) ?: "")!!,
        glyphSection.getStringList("placeholders"),
        glyphSection.getBoolean("tabcomplete"),
        glyphSection.getBoolean("is_emoji")
    )

    fun copy(
        id: String = this.id,
        font: Key = this.font,
        texture: Key = this.texture,
        ascent: Int = this.ascent,
        height: Int = this.height,
        unicodes: List<String> = this.unicodes,
        permission: String = this.permission,
        placeholders: List<String> = this.placeholders,
        tabcomplete: Boolean = this.tabcomplete,
        isEmoji: Boolean = this.isEmoji
    ): Glyph = Glyph(
        id = id,
        font = font,
        texture = texture,
        ascent = ascent,
        height = height,
        unicodes = unicodes,
        permission = permission,
        placeholders = placeholders,
        tabcomplete = tabcomplete,
        isEmoji = isEmoji
    )

    companion object {
        val ORIGINAL_ITEM_RENAME = NamespacedKey.fromString("nexo:original_item_rename")!!
        val REQUIRED_GLYPH = Key.key("minecraft:required/exit_icon.png")
        val REQUIRED_CHAR = Char(41999).toString()

        val assignedGlyphUnicodes: MutableMap<String, List<String>> = Object2ObjectOpenHashMap<String, List<String>>()
        fun definedUnicodes(glyphSection: ConfigurationSection): List<String>? {
            return (glyphSection.getStringOrNull("char")?.let(::listOf) ?: glyphSection.getStringListOrNull("char"))
        }
        fun calculateGlyphUnicodes(glyphSection: ConfigurationSection): List<String> {
            return assignedGlyphUnicodes.computeIfAbsent(glyphSection.name) {
                definedUnicodes(glyphSection) ?: let {
                    var min = 42000
                    val usedCharIds = assignedGlyphUnicodes.values.flatten().flatMap { it.map(Char::code) }.sorted().toMutableSet()
                    val (rows, columns) = glyphSection.getInt("rows", 1) to glyphSection.getInt("columns", 1)

                    (0 until rows * columns).map { index ->
                        while (min in usedCharIds) min++
                        min.apply(usedCharIds::add).toChar()
                    }.chunked(columns).map { it.joinToString("") }.also { unicodes ->
                        if (Settings.DISABLE_AUTOMATIC_GLYPH_CODE.toBool()) return@also
                        glyphSection.set("char", if (unicodes.size == 1) unicodes.first() else unicodes)
                        glyphSection.remove("rows").remove("columns")
                    }
                }
            }
        }

        fun containsTagOrPlaceholder(string: String): Boolean {
            return GlyphTag.containsTag(string) || NexoPlugin.instance().fontManager().placeholderGlyphMap.keys.any(string::contains)
        }
    }

    fun hasPermission(player: Player?) = player == null || permission.isEmpty() || player.hasPermission(permission)

    /**
     * Useful to easily get the MiniMessage-tag for a glyph
     */
    val glyphTag = "<glyph:$id>"

    @JvmOverloads
    fun glyphComponent(colorable: Boolean = false, shadow: GlyphShadow? = null, bitmapIndexRange: IntRange = IntRange.EMPTY): Component {
        return if (bitmapIndexRange == IntRange.EMPTY) component
            .run { if (!colorable) color(defaultColor).children(children().map { it.color(defaultColor) }) else this }
            .glyphShadow(shadow)
        else bitmapComponent(bitmapIndexRange, colorable, shadow)
    }

    open val fontProviders: Array<FontProvider> by lazy {
        val unicodes = unicodes.ifEmpty { RequiredGlyph.unicodes }
        arrayOf(FontProvider.bitMap(texture, height, ascent.coerceAtMost(height), unicodes))
    }
}
