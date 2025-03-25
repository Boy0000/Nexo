package com.nexomc.nexo.fonts

import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.KeyUtils.appendSuffix
import com.nexomc.nexo.utils.getKey
import com.nexomc.nexo.utils.getStringListOrNull
import com.nexomc.nexo.utils.getStringOrNull
import com.nexomc.nexo.utils.toIntRangeOrNull
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import java.util.regex.Pattern
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import team.unnamed.creative.font.Font
import team.unnamed.creative.font.FontProvider

object RequiredGlyph : Glyph("required", Font.MINECRAFT_DEFAULT, REQUIRED_GLYPH, 8, 8, REQUIRED_CHAR)

data class ReferenceGlyph(val glyph: Glyph, val referenceId: String, val index: IntRange, override val permission: String)
    : Glyph(referenceId, glyph.font, glyph.texture, glyph.ascent, glyph.height, listOf(glyph.unicodes.joinToString("").substring(index.first - 1, index.last)), permission, listOf())

open class Glyph(
    val id: String,
    val font: Key,
    var texture: Key,
    val ascent: Int,
    val height: Int,
    val unicodes: List<String>,

    open val permission: String = "",
    val placeholders: List<String> = listOf(),
    val tabcomplete: Boolean = false,
    val isEmoji: Boolean = false,
) {

    private val chars = unicodes.flatMap { it.toList() }.toCharArray()
    val formattedUnicodes = unicodes.joinToString("\n") { it.toList().joinToString(Shift.of(-1)) }
    private val unicodeComponent = Component.textOfChildren(*unicodes.flatMapIndexed { i, row ->
        listOfNotNull(Component.text(row.toList().joinToString(Shift.of(-1)), NamedTextColor.WHITE).font(font), Component.newline().takeIf { unicodes.size != i + 1 })
    }.toTypedArray())
    private val colorableUnicodeComponent = Component.textOfChildren(*unicodes.flatMap {
        listOf(Component.text(it.toList().joinToString(Shift.of(-1))).font(font), Component.newline())
    }.toTypedArray())
    private fun bitmapComponent(bitmapIndex: Int, colorable: Boolean = false, shift: String = Shift.of(0)) =
        Component.text("${chars.elementAtOrNull(bitmapIndex - 1) ?: chars.first()}$shift", NamedTextColor.WHITE.takeUnless { colorable }).font(font)
    private fun bitmapComponent(indexRange: IntRange, colorable: Boolean = false) =
        Component.textOfChildren(*indexRange.map { bitmapComponent(it, colorable, Shift.of(-1).takeIf { indexRange.count() > 1 } ?: "") }.toTypedArray())

    val baseRegex: Pattern
    private val escapedRegex: Pattern
    val replacementConfig: TextReplacementConfig
    val placeholderReplacementConfig: TextReplacementConfig?
    val escapePlaceholderReplacementConfig: TextReplacementConfig?
    val escapeReplacementConfig: TextReplacementConfig
    val unescapeReplacementConfig: TextReplacementConfig

    constructor(id: String, font: Key, texture: Key, ascent: Int, height: Int, unicode: String) : this(id, font, texture, ascent, height, listOf(unicode))

    constructor(glyphSection: ConfigurationSection) : this(
        glyphSection.name,
        glyphSection.getKey("font", Font.MINECRAFT_DEFAULT),
        glyphSection.getKey("texture", REQUIRED_GLYPH).appendSuffix(".png"),
        glyphSection.getInt("ascent", 8),
        glyphSection.getInt("height", 8),
        calculateGlyphUnicodes(glyphSection),

        glyphSection.getString("chat.permission") ?: "",
        glyphSection.getStringList("chat.placeholders"),
        glyphSection.getBoolean("chat.tabcomplete"),
        glyphSection.getBoolean("is_emoji")
    )

    init {
        val baseRegex = "((<(glyph|g):$id)(:(c|colorable|\\d))*>)"
        this.baseRegex = Pattern.compile("(?<!\\\\)$baseRegex")
        escapedRegex = Pattern.compile("\\\\" + baseRegex)
        replacementConfig = TextReplacementConfig.builder().match(this.baseRegex).replacement { match, builder ->
            val args = match.group(1).substringAfter("<glyph:").substringAfter("<g:").substringBefore(">").split(":")
            val colorable = args.any { it == "colorable" || it == "c" }
            val bitmapIndex = args.firstNotNullOfOrNull { it.toIntRangeOrNull() ?: it.toIntOrNull()?.let { i ->IntRange(i, i) } } ?: IntRange.EMPTY
            glyphComponent(colorable, bitmapIndex)
        }.build()
        placeholderReplacementConfig = TextReplacementConfig.builder().takeIf { placeholders.isNotEmpty() }
            ?.match("(${placeholders.joinToString("|", transform = Regex::escape)})")
            ?.replacement(glyphComponent())?.build()
        escapePlaceholderReplacementConfig = TextReplacementConfig.builder().takeIf { placeholders.isNotEmpty() }
            ?.match("(${placeholders.joinToString("|", transform = Regex::escape)})")
            ?.replacement { match, b ->
                b.content("\\\\${match.group(1)}")
            }?.build()

        escapeReplacementConfig = TextReplacementConfig.builder().match(baseRegex).replacement { match, b ->
            b.content("\\\\${match.group(1)}")
        }.build()
        unescapeReplacementConfig = TextReplacementConfig.builder().match(escapedRegex).replacement { match, b ->
            b.content(match.group(1).removePrefix("\\"))
        }.build()
    }

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

                    (0 until rows).map { row ->
                        (0 until columns).map { column ->
                            while (min in usedCharIds) min++
                            usedCharIds.add(min)
                            min.toChar()
                        }.joinToString("")
                    }.also { unicodes ->
                        if (Settings.DISABLE_AUTOMATIC_GLYPH_CODE.toBool()) return@also
                        if (unicodes.size == 1) glyphSection.set("char", unicodes.first())
                        else glyphSection.set("char", unicodes)
                        glyphSection.set("rows", null)
                        glyphSection.set("columns", null)
                    }
                }
            }
        }
    }

    fun hasPermission(player: Player?) = player == null || permission.isEmpty() || player.hasPermission(permission)

    /**
     * Useful to easily get the MiniMessage-tag for a glyph
     */
    fun glyphTag() = "<glyph:$id>"

    @JvmOverloads
    fun glyphComponent(colorable: Boolean = false, bitmapIndexRange: IntRange = IntRange.EMPTY): Component {
        return when {
            bitmapIndexRange == IntRange.EMPTY -> if (colorable) colorableUnicodeComponent else unicodeComponent
            else -> bitmapComponent(bitmapIndexRange, colorable)
        }
    }

    val fontProvider by lazy { FontProvider.bitMap().file(texture).height(height).ascent(ascent.coerceAtMost(height)).characters(unicodes).build() }
}
