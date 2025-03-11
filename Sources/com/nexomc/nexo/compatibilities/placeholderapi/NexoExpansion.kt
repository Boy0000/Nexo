package com.nexomc.nexo.compatibilities.placeholderapi

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.fonts.RequiredGlyph
import com.nexomc.nexo.fonts.Shift
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import net.kyori.adventure.key.Key
import org.bukkit.OfflinePlayer

class NexoExpansion(private val plugin: NexoPlugin) : PlaceholderExpansion() {
    override fun getAuthor() = "boy0000"

    override fun getIdentifier() = "nexo"

    override fun getVersion(): String = plugin.pluginMeta.version

    override fun persist() = true

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        val glyph = plugin.fontManager().glyphFromName(params)

        return when {
            glyph !is RequiredGlyph -> if (glyph.font == DEFAULT_FONT) glyph.formattedUnicodes else glyph.glyphTag()
            params.startsWith("shift_") -> Shift.of(params.substringAfter("shift_").toIntOrNull() ?: return null)
            params == "pack_hash" -> plugin.packGenerator().builtPack()?.hash()
            else -> ""
        }
    }

    private val DEFAULT_FONT = Key.key("default")
}
