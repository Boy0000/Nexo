package com.nexomc.nexo.items

import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.toIntRangeOrNull
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.kyori.adventure.key.Key
import org.bukkit.Material
import java.util.*

class CustomModelData(val type: Material, nexoMeta: NexoMeta, val customModelData: Int) {

    init {
        val map = DATAS.computeIfAbsent(type) { Object2ObjectOpenHashMap() }
        nexoMeta.model?.let {
            map[it] = customModelData
        } ?: Logs.logWarn("Failed to assign customModelData due to invalid model")

    }

    companion object {
        val DATAS = Object2ObjectOpenHashMap<Material, MutableMap<Key, Int>>()

        fun generateId(model: Key, type: Material): Int {
            val startingCMD = Settings.INITIAL_CUSTOM_MODEL_DATA.toInt(1000)
            val usedModelDatas = DATAS.getOrPut(type) { Object2ObjectOpenHashMap() }

            usedModelDatas[model]?.let { return it }

            val usedValues = usedModelDatas.values.toSet()
            val skipped = skippedCustomModelData(type)
            val currentMax = (usedValues.maxOrNull() ?: startingCMD).coerceAtLeast(startingCMD)

            for (cmd in startingCMD..currentMax) {
                if (cmd !in usedValues && cmd !in skipped) {
                    return cmd.also { usedModelDatas[model] = it }
                }
            }

            return nextNotSkippedCustomModelData(type, currentMax + 1).also {
                usedModelDatas[model] = it
            }
        }


        private fun nextNotSkippedCustomModelData(type: Material, start: Int): Int {
            val skipped = skippedCustomModelData(type)
            var candidate = start
            while (candidate in skipped) candidate++
            return candidate
        }

        private fun skippedCustomModelData(type: Material): SortedSet<Int> {
            val skippedData = sortedSetOf<Int>()
            val section = Settings.SKIPPED_MODEL_DATA_NUMBERS.toConfigSection() ?: return skippedData

            fun parseString(input: String) {
                when {
                    ".." in input -> input.toIntRangeOrNull()?.forEach(skippedData::add)
                        ?: Logs.logError("Invalid range for ${type.name} in settings.yml")
                    else -> input.toIntOrNull()?.let(skippedData::add)
                        ?: Logs.logError("Invalid number for ${type.name} in settings.yml")
                }
            }

            section.getString(type.name.lowercase())?.let(::parseString)
            section.getString(type.name)?.let(::parseString)

            section.getStringList(type.name.lowercase()).forEach(::parseString)

            return skippedData
        }
    }
}
