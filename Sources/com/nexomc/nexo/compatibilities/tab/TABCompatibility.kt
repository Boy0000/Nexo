package com.nexomc.nexo.compatibilities.tab

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.utils.PluginUtils
import com.nexomc.nexo.utils.serializeStrict
import me.neznamy.tab.api.TabAPI
import me.neznamy.tab.api.event.plugin.PlaceholderRegisterEvent

object TABCompatibility {

    fun registerPlaceholders() {
        runCatching {
            // if TAB is on backend, override PlaceholderAPI Placeholders
            if (PluginUtils.isEnabled("TAB")) TabAPI.getInstance().eventBus?.register(PlaceholderRegisterEvent::class.java) { event ->
                val glyphs = NexoPlugin.instance().fontManager().glyphs().associate { "%nexo_${it.id}%" to it.glyphComponent().serializeStrict() }
                val placeholder = glyphs[event.identifier] ?: return@register
                event.setServerPlaceholder { placeholder }
            }
        }
    }
}