package com.nexomc.nexo.items

import org.bukkit.configuration.ConfigurationSection

object ItemTemplate {

    val itemTemplates = mutableMapOf<String, ItemParser>()

    fun register(section: ConfigurationSection) {
        section.set("injectId", false)
        itemTemplates[section.name] = ItemParser(section)
    }

    fun parserTemplate(id: String): ItemParser? {
        return itemTemplates[id]
    }

    fun isTemplate(id: String?): Boolean {
        return itemTemplates.containsKey(id)
    }
}
