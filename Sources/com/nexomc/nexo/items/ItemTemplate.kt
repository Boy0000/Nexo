package com.nexomc.nexo.items

import com.nexomc.nexo.utils.NexoYaml
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration

object ItemTemplate {

    val itemTemplates = mutableMapOf<String, ConfigurationSection>()

    fun register(section: ConfigurationSection) {
        section.set("injectId", false)
        itemTemplates[section.name] = section
    }

    fun parserTemplate(id: String): ItemParser? {
        return itemTemplates[id]?.let(::ItemParser)
    }

    fun parserTemplates(ids: List<String>): ItemParser? {
        val templates = ids.mapNotNull { itemTemplates[it] }.takeUnless { it.isEmpty() } ?: return null

        val mergedSection = templates.fold(YamlConfiguration()) { acc, template ->
            acc.apply { NexoYaml.copyConfigurationSection(template, this) }
        }

        return ItemParser(mergedSection)
    }

    fun isTemplate(id: String?): Boolean {
        return itemTemplates.containsKey(id)
    }
}
