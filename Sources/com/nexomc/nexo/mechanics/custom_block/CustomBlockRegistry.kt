package com.nexomc.nexo.mechanics.custom_block

import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.safeCast
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.configuration.ConfigurationSection

object CustomBlockRegistry {
    val registry = linkedMapOf<String, CustomBlockType<out CustomBlockMechanic>>()

    fun <T : CustomBlockMechanic> getByClass(mechanicClass: Class<T>): CustomBlockType<CustomBlockMechanic>? {
        return registry.values.find { it.clazz == mechanicClass }.safeCast()
    }


    fun getMechanic(itemId: String): CustomBlockMechanic? {
        return registry.firstNotNullOfOrNull { it.value.factory()?.getMechanic(itemId) as? CustomBlockMechanic }
    }

    fun getMechanic(block: Block): CustomBlockMechanic? {
        return registry.firstNotNullOfOrNull { it.value.getMechanic(block) }
    }

    fun getMechanic(blockData: BlockData): CustomBlockMechanic? {
        return registry.firstNotNullOfOrNull { it.value.getMechanic(blockData) }
    }

    fun <T : CustomBlockMechanic> register(blockType: CustomBlockType<T>) {
        registry[blockType.name()] = blockType
    }

    fun get(name: String?) = registry[name]

    val names: List<String> get() = registry.keys.toList()

    val types: List<CustomBlockType<*>>
        get() = registry.values.toList()

    fun fromMechanicSection(section: ConfigurationSection): CustomBlockType<*>? {
        val type = get(section.getString("type"))
        if (type == null) {
            val itemId = section.parent?.parent?.name ?: return null
            Logs.logError("No CustomBlock-type defined in $itemId")
            Logs.logError("Valid types are: ${names.joinToString()}")
        }
        return type
    }
}
