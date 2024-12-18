package com.nexomc.nexo.items

import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.toIntRangeOrNull
import net.kyori.adventure.key.Key
import org.bukkit.Material

class CustomModelData(val type: Material, nexoMeta: NexoMeta, val customModelData: Int) {

    init {
        DATAS.compute(type) { _: Material, datas: MutableMap<Key, Int>? ->
            (datas ?: mutableMapOf()).apply {
                if (nexoMeta.modelKey == null) {
                    Logs.logWarn("Failed to assign customModelData due to invalid model")
                }
                put(nexoMeta.modelKey ?: return@apply, customModelData)
            }
        }
    }

    companion object {
        val DATAS: MutableMap<Material, MutableMap<Key, Int>> = mutableMapOf()

        fun generateId(model: Key, type: Material): Int {
            val STARTING_CMD = Settings.INITIAL_CUSTOM_MODEL_DATA.toInt(1000)
            var usedModelDatas = mutableMapOf<Key, Int>()
            if (type !in DATAS && STARTING_CMD !in skippedCustomModelData(type)) {
                usedModelDatas[model] = STARTING_CMD
                DATAS[type] = usedModelDatas
                return STARTING_CMD
            } else usedModelDatas = DATAS.getOrDefault(type, HashMap())

            usedModelDatas[model]?.let { return it }

            val currentHighestModelData = usedModelDatas.values.max()
            for (i in STARTING_CMD..currentHighestModelData) {
                if (!usedModelDatas.containsValue(i)) { // if the id is available
                    if (i in skippedCustomModelData(type)) continue  // if the id should be skipped

                    usedModelDatas[model] = i
                    DATAS[type] = usedModelDatas
                    return i
                }
            }

            val newHighestModelData = nextNotSkippedCustomModelData(type, currentHighestModelData + 1)
            usedModelDatas[model] = newHighestModelData
            DATAS[type] = usedModelDatas
            return newHighestModelData
        }

        private fun nextNotSkippedCustomModelData(type: Material, i: Int): Int {
            val sorted = ArrayList(skippedCustomModelData(type))
            sorted.sortWith(Comparator.naturalOrder())
            return if (i !in sorted) i else sorted.first { it > i }
        }

        private fun skippedCustomModelData(type: Material): Set<Int> {
            val skippedCustomModelData = mutableSetOf<Int>()
            val section = Settings.SKIPPED_MODEL_DATA_NUMBERS.toConfigSection()
            if (section?.get(type.name) == null) return skippedCustomModelData

            val skippedString = section.getString(type.name.lowercase()) ?: section.getString(type.name)
            if (skippedString != null) {
                if (".." in skippedString) {
                    skippedString.toIntRangeOrNull()?.forEach(skippedCustomModelData::add) ?: apply {
                        Logs.logError("Invalid skipped model-data range for ${type.name} in settings.yml")
                        return skippedCustomModelData
                    }
                } else skippedString.toIntOrNull()?.let(skippedCustomModelData::add)
                    ?: Logs.logError("Invalid skipped model-data number for ${type.name} in settings.yml")
            } else section.getStringList(type.name.lowercase()).forEach { s: String ->
                if (".." in s) runCatching {
                    s.toIntRangeOrNull()?.forEach(skippedCustomModelData::add)
                }.onFailure {
                    Logs.logError("Invalid skipped model-data range for " + type.name + " in settings.yml")
                } else skippedCustomModelData += s.toIntOrNull()
                    ?: return@forEach Logs.logError("Invalid skipped model-data number for " + type.name + " in settings.yml")
            }

            return skippedCustomModelData
        }
    }
}
