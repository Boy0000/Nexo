package com.nexomc.nexo.mechanics.custom_block

import com.nexomc.nexo.utils.logs.Logs
import org.bukkit.configuration.ConfigurationSection

object CustomBlockRegistry {
    private val registry = linkedMapOf<String, CustomBlockType>()

    fun register(blockType: CustomBlockType) {
        registry[blockType.name()] = blockType
    }

    fun get(name: String?) = registry[name]

    val names: List<String> get() = registry.keys.toList()

    val types: List<CustomBlockType>
        get() = registry.values.toList()

    fun fromMechanicSection(section: ConfigurationSection): CustomBlockType? {
        val type = get(section.getString("type"))
        if (type == null) {
            val itemId = section.parent?.parent?.name ?: return null
            Logs.logError("No CustomBlock-type defined in $itemId")
            Logs.logError("Valid types are: ${names.joinToString()}")
        }
        return type
    }
}
