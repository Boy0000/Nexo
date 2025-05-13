package com.nexomc.nexo.fonts

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.ConfigsManager
import com.nexomc.nexo.configs.Settings
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList

class FontManager(configsManager: ConfigsManager) {
    private val glyphMap: Object2ObjectOpenHashMap<String, Glyph> = Object2ObjectOpenHashMap()
    val placeholderGlyphMap: Object2ObjectOpenHashMap<String, Glyph> = Object2ObjectOpenHashMap()
    val unicodeGlyphMap: Object2ObjectOpenHashMap<Char, String> = Object2ObjectOpenHashMap()
    val tabcompletions: ObjectOpenHashSet<String> = ObjectOpenHashSet()
    private val fontListener: FontListener = FontListener(this)

    init {
        loadGlyphs(configsManager.parseGlyphConfigs())
    }

    fun registerEvents() {
        Bukkit.getPluginManager().registerEvents(fontListener, NexoPlugin.instance())
    }

    fun unregisterEvents() {
        HandlerList.unregisterAll(fontListener)
    }

    private fun loadGlyphs(glyphs: Collection<Glyph>) {
        val unicodeTabcompletion = Settings.UNICODE_COMPLETIONS.toBool()
        glyphs.forEach { glyph: Glyph ->
            if (glyph.unicodes.none(String::isNotEmpty)) return@forEach
            glyphMap[glyph.id] = glyph
            if (glyph is ReferenceGlyph) return@forEach
            for (unicodes in glyph.unicodes) for (char in unicodes) unicodeGlyphMap[char] = glyph.id
            for (placeholder in glyph.placeholders) placeholderGlyphMap[placeholder] = glyph
            if (glyph.tabcomplete) if (unicodeTabcompletion) tabcompletions.add(glyph.formattedUnicodes) else tabcompletions.addAll(glyph.placeholders)
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

    fun sendGlyphTabCompletion(player: Player) {
        player.removeCustomChatCompletions(tabcompletions)
        player.addCustomChatCompletions(tabcompletions)
    }
}
