package com.nexomc.nexo.mechanics.custom_block

import com.nexomc.nexo.utils.getKey
import com.nexomc.nexo.utils.getStringOrNull
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.rootId
import com.nexomc.nexo.utils.rootSection
import com.nexomc.nexo.utils.safeCast
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.kyori.adventure.key.Key
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.configuration.ConfigurationSection

typealias CustomBlockName = String

object CustomBlockRegistry {
    val DATAS = Object2ObjectOpenHashMap<CustomBlockName, Object2IntLinkedOpenHashMap<Key>>()
    val registry = linkedMapOf<CustomBlockName, CustomBlockType<out CustomBlockMechanic>>()

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

    fun generateVariation(section: ConfigurationSection): Int? {
        val model = section.getKey("model") ?: section.rootSection.getKey("Pack.model") ?: Key.key(section.rootId)
        val type = section.getStringOrNull("type")

        return when {
            type != null -> generateVariation(model, type)
            else -> {
                Logs.logWarn("Could not assign <yellow><i>custom_variation</i></yellow> automatically to <red>${section.rootId}</red> as it was missing <yellow><i>type</i></yellow>-property")
                null
            }
        }
    }

    fun generateVariation(model: Key, type: CustomBlockName): Int {
        val usedModels = DATAS.getOrPut(type) { Object2IntLinkedOpenHashMap() }

        if (usedModels.containsKey(model)) return usedModels.getOrDefault(model, 1)

        val usedVariations = usedModels.values.toSet()
        val currentMax = (usedVariations.maxOrNull() ?: 1).coerceAtLeast(1)

        for (v in 1..currentMax) {
            if (v !in usedVariations) {
                return v.also { usedModels[model] = it }
            }
        }

        val variation = currentMax + 1
        usedModels[model] = variation
        return variation
    }
}
