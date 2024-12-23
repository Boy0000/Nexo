package com.nexomc.nexo.fonts

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.ConfigsManager
import com.nexomc.nexo.configs.Settings
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import team.unnamed.creative.font.BitMapFontProvider
import team.unnamed.creative.font.FontProvider
import java.util.*

class FontManager(configsManager: ConfigsManager) {
    private val glyphMap: MutableMap<String, Glyph> = LinkedHashMap()
    val placeholderGlyphMap: MutableMap<String, Glyph> = LinkedHashMap()
    val unicodeGlyphMap: MutableMap<Char, String> = LinkedHashMap()
    private val fontListener: FontListener = FontListener(this)

    init {
        configsManager.bitmaps.getConfigurationSection("bitmaps")?.let {
            glyphBitMaps = it.getKeys(false).mapNotNull { key ->
                val section = it.getConfigurationSection(key) ?: return@mapNotNull null
                key to GlyphBitMap(
                    Key.key(section.getString("font", "minecraft:default")!!),
                    Key.key(section.getString("texture", "")!!.replace("^(?!.*\\.png$)", "") + ".png"),
                    section.getInt("rows"), section.getInt("columns"),
                    section.getInt("height", 8), section.getInt("ascent", 8)
                )
            }.toMap()
        }
        loadGlyphs(configsManager.parseGlyphConfigs())
    }

    fun registerEvents() {
        Bukkit.getPluginManager().registerEvents(fontListener, NexoPlugin.instance())
        fontListener.registerChatHandlers()
    }

    fun unregisterEvents() {
        HandlerList.unregisterAll(fontListener)
        fontListener.unregisterChatHandlers()
    }

    private fun loadGlyphs(glyphs: Collection<Glyph>) {
        glyphs.forEach { glyph: Glyph ->
            if (glyph.character().isBlank()) return@forEach
            glyphMap[glyph.id] = glyph
            unicodeGlyphMap[glyph.character().first()] = glyph.id
            for (placeholder in glyph.placeholders) placeholderGlyphMap[placeholder] = glyph
        }
    }

    fun glyphs(): Collection<Glyph> = glyphMap.values

    fun emojis() = glyphMap.values.filter(Glyph::isEmoji)

    /**
     * Get a Glyph from a given Glyph-ID
     *
     * @param id The Glyph-ID
     * @return Returns the Glyph if it exists, otherwise the required Glyph
     */
    fun glyphFromName(id: String?) = glyphMap[id] ?: glyphMap["required"]!!

    /**
     * Get a Glyph from a given Glyph-ID
     *
     * @param id The Glyph-ID
     * @return Returns the Glyph if it exists, otherwise null
     */
    fun glyphFromID(id: String?) = glyphMap[id]

    fun glyphFromPlaceholder(word: String?) = placeholderGlyphMap[word]

    private val currentGlyphCompletions = mutableMapOf<UUID, List<String>>()

    fun sendGlyphTabCompletion(player: Player) {
        val completions = placeholderGlyphMap.values
            .filter(Glyph::tabcomplete)
            .map { glyph: Glyph ->
                if (Settings.UNICODE_COMPLETIONS.toBool()) listOf(glyph.character())
                else glyph.placeholders.toList()
            }.flatten()

        player.removeCustomChatCompletions(currentGlyphCompletions.getOrDefault(player.uniqueId, ArrayList()))
        player.addCustomChatCompletions(completions)
        currentGlyphCompletions[player.uniqueId] = completions
    }

    fun clearGlyphTabCompletions(player: Player) {
        currentGlyphCompletions.remove(player.uniqueId)
    }

    @JvmRecord
    data class GlyphBitMap(
        val font: Key,
        val texture: Key,
        val rows: Int,
        val columns: Int,
        val height: Int,
        val ascent: Int
    ) {
        fun fontProvider(): BitMapFontProvider {
            val bitmapGlyphs = NexoPlugin.instance().fontManager().glyphs().filter { it.hasBitmap() && it.bitmap() == this }
            val charMap = ArrayList<String>(rows)

            (1..rows).forEach { currentRow ->
                val glyphsInRow: List<Glyph> = bitmapGlyphs.filter { g -> g.bitmapEntry?.row == currentRow }
                val charRow = StringBuilder()
                (1..columns)
                    .asSequence()
                    .map { glyphsInRow.firstOrNull { g -> g.bitmapEntry?.column == it } }
                    .forEach { charRow.append(it?.character() ?: Glyph.WHITESPACE_GLYPH) }
                charMap.add(currentRow - 1, charRow.toString())
            }

            return FontProvider.bitMap(texture, height, ascent, charMap)
        }
    }

    companion object {
        var glyphBitMaps = mapOf<String, GlyphBitMap>()
        fun glyphBitMap(id: String?) = glyphBitMaps[id]
    }
}
