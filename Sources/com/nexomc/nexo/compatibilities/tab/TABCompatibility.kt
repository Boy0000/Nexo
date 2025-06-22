package com.nexomc.nexo.compatibilities.tab

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.utils.PluginUtils
import com.nexomc.nexo.utils.serializeStrict
import me.neznamy.tab.api.TabAPI
import me.neznamy.tab.api.event.plugin.PlaceholderRegisterEvent

object TABCompatibility {

    fun registerPlaceholders() {
        runCatching {
            val glyphs = NexoPlugin.instance().fontManager().glyphs().associate { "%nexo_${it.id}%" to it.glyphComponent().serializeStrict() }

            // if TAB is on backend, override PlaceholderAPI Placeholders
            if (PluginUtils.isEnabled("TAB")) TabAPI.getInstance().eventBus?.register(PlaceholderRegisterEvent::class.java) { event ->
                val placeholder = glyphs[event.identifier] ?: return@register
                event.setServerPlaceholder { placeholder }
            }

            // if TAB is not on backend server and TAB-Bridge is not installed, add server-placeholders
            if (!PluginUtils.isEnabled("TAB") && !PluginUtils.isEnabled("TAB-Bridge")) glyphs.forEach { id, component ->
                TabAPI.getInstance().placeholderManager.registerServerPlaceholder(id, -1) { component }
            }
        }
    }
}