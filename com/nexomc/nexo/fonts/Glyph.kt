package com.nexomc.nexo.fonts

import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.AdventureUtils
import com.nexomc.nexo.utils.appendIfMissing
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.event.HoverEventSource
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import team.unnamed.creative.font.FontProvider
import java.util.*
import java.util.regex.Pattern


open class Glyph {
    var isFileChanged = false
    val id: String
    val font: Key
    val isEmoji: Boolean
    val tabcomplete: Boolean
    private val character: Char?
    var texture: Key
    val ascent: Int
    val height: Int
    val permission: String
    val placeholders: Array<String>
    val bitmapEntry: BitMapEntry?
    val baseRegex: Pattern
    val escapedRegex: Pattern
    val replacementConfig: TextReplacementConfig
    val placeholderReplacementConfig: TextReplacementConfig?

    constructor(id: String, glyphSection: ConfigurationSection, newChars: Char) {
        this.id = id

        isEmoji = glyphSection.getBoolean("is_emoji", false)

        val chatSection = glyphSection.getConfigurationSection("chat")
        placeholders = chatSection?.getStringList("placeholders")?.toTypedArray() ?: emptyArray()
        permission = chatSection?.getString("permission") ?: ""
        tabcomplete = chatSection?.getBoolean("tabcomplete", false) ?: false

        if ("code" in glyphSection) {
            if (glyphSection.isInt("code")) glyphSection.set("char", glyphSection.getInt("code").toChar())
            glyphSection.set("code", null)
            isFileChanged = true
        }

        if ("char" !in glyphSection && !Settings.DISABLE_AUTOMATIC_GLYPH_CODE.toBool()) {
            glyphSection.set("char", newChars)
            isFileChanged = true
        }

        character = glyphSection.getString("char", "")?.firstOrNull()

        bitmapEntry = glyphSection.getConfigurationSection("bitmap")?.let {
            BitMapEntry(it.getString("id"), it.getInt("row"), it.getInt("column"))
        }
        val bitmap = bitmap()
        ascent = bitmap?.ascent ?: glyphSection.getInt("ascent", 8)
        height = bitmap?.height ?: glyphSection.getInt("height", 8)
        texture = bitmap?.texture ?: Key.key(glyphSection.getString("texture", "required/exit_icon")!!.appendIfMissing(".png"))
        font = bitmap?.font ?: Key.key(glyphSection.getString("font", "minecraft:default")!!)

        val baseRegex = "((<(glyph|g):$id)(:(c|colorable))*>)"
        this.baseRegex = Pattern.compile("(?<!\\\\)$baseRegex")
        escapedRegex = Pattern.compile("\\\\" + baseRegex)
        replacementConfig = TextReplacementConfig.builder().match(this.baseRegex).replacement(glyphComponent()).build()
        placeholderReplacementConfig = TextReplacementConfig.builder().takeIf { placeholders.isNotEmpty() }
            ?.match("(${placeholders.joinToString("|", transform = Regex::escape)})")
            ?.replacement(glyphComponent())?.build()
    }

    constructor(
        id: String,
        character: Char?,
        ascent: Int,
        height: Int,
        texture: Key,
        font: Key,
        isEmoji: Boolean,
        placeholders: List<String>,
        permission: String?,
        tabcomplete: Boolean,
        bitmapEntry: BitMapEntry?
    ) {
        this.id = id
        this.ascent = ascent
        this.height = height
        this.texture = texture
        this.font = font
        this.character = character
        this.isEmoji = isEmoji
        this.placeholders = placeholders.toTypedArray()
        this.permission = permission ?: ""
        this.tabcomplete = tabcomplete
        this.bitmapEntry = bitmapEntry

        val baseRegex = "((<(glyph|g):$id)(:(c|colorable))*>)"
        this.baseRegex = Pattern.compile("(?<!\\\\)$baseRegex")
        escapedRegex = Pattern.compile("\\\\" + baseRegex)
        replacementConfig = TextReplacementConfig.builder().match(baseRegex).replacement(glyphComponent()).build()
        placeholderReplacementConfig = TextReplacementConfig.builder().takeIf { placeholders.isNotEmpty() }
            ?.match("(${placeholders.joinToString("|", transform = Regex::escape)})")
            ?.replacement(glyphComponent())?.build()
    }

    @JvmRecord
    data class BitMapEntry(val id: String?, val row: Int, val column: Int)

    val bitmapId get() = bitmapEntry?.id

    fun hasBitmap() = bitmapId != null

    val isBitMap: Boolean
        get() = FontManager.glyphBitMap(bitmapId) != null

    fun bitmap() = FontManager.glyphBitMap(bitmapId)

    fun character() = character?.toString() ?: ""

    fun fontProvider() = FontProvider.bitMap()
        .file(texture)
        .height(height)
        .ascent(ascent.coerceAtMost(height))
        .characters(listOf(character.toString()))
        .build()

    fun hasPermission(player: Player?) = player == null || permission.isEmpty() || player.hasPermission(permission)

    /**
     * Useful to easily get the MiniMessage-tag for a glyph
     */
    fun glyphTag() = "<glyph:$id>"

    fun glyphComponent() = Component.textOfChildren(Component.text(character!!, NamedTextColor.WHITE).font(font))

    fun glyphComponent(colorable: Boolean) = Component.textOfChildren(Component.text(character!!).font(font).hoverEvent(glyphHoverText()))

    fun glyphHoverText(): HoverEventSource<*>? {
        val hoverText = Settings.GLYPH_HOVER_TEXT.toString()
        val hoverResolver = TagResolver.builder().tag("glyph_placeholder", Tag.selfClosingInserting(
            Component.text(Arrays.stream(placeholders).findFirst().orElse(""))
        )).build()

        val hoverComponent = AdventureUtils.MINI_MESSAGE.deserialize(hoverText, hoverResolver)
        return hoverComponent.takeUnless { hoverText.isEmpty() || it === Component.empty() }?.let { HoverEvent.showText(it) }
    }

    fun font() = font

    val isRequired: Boolean
        get() = this.id == "required"

    class RequiredGlyph(character: Char) : Glyph(
        "required",
        character,
        8,
        8,
        REQUIRED_GLYPH,
        Key.key("minecraft:default"),
        false,
        mutableListOf(),
        "nexo.emoji.required",
        false,
        null
    )

    companion object {
        const val WHITESPACE_GLYPH = '\ue000'
        val REQUIRED_GLYPH = Key.key("minecraft:required/exit_icon.png")
    }
}
