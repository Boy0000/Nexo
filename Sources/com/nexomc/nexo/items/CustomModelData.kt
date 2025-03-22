package com.nexomc.nexo.items

import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.toIntRangeOrNull
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import java.util.SortedSet
import net.kyori.adventure.key.Key
import org.bukkit.Material

class CustomModelData(val type: Material, nexoMeta: NexoMeta, val customModelData: Int) {

    init {
        DATAS.compute(type) { _, datas ->
            (datas ?: mutableMapOf()).apply {
                nexoMeta.model?.let { put(it, customModelData) }
                    ?: Logs.logWarn("Failed to assign customModelData due to invalid model")
            }
        }
    }

    companion object {
        val DATAS = Object2ObjectOpenHashMap<Material, MutableMap<Key, Int>>()

        fun generateId(model: Key, type: Material): Int {
            val startingCMD = Settings.INITIAL_CUSTOM_MODEL_DATA.toInt(1000)
            val usedModelDatas = DATAS.getOrDefault(type, Object2ObjectOpenHashMap())

            usedModelDatas[model]?.let { return it }

            val currentHighestModelData = usedModelDatas.values.maxOrNull() ?: startingCMD
            val skippedData = skippedCustomModelData(type)

            for (i in startingCMD..currentHighestModelData) {
                if (i !in usedModelDatas.values && i !in skippedData) {
                    usedModelDatas[model] = i
                    DATAS[type] = usedModelDatas
                    return i
                }
            }

            return nextNotSkippedCustomModelData(type, currentHighestModelData + 1).also {
                usedModelDatas[model] = it
                DATAS[type] = usedModelDatas
            }
        }

        private fun nextNotSkippedCustomModelData(type: Material, start: Int): Int {
            return skippedCustomModelData(type).firstOrNull { it > start } ?: start
        }

        private fun skippedCustomModelData(type: Material): SortedSet<Int> {
            val skippedData = sortedSetOf<Int>()
            val section = Settings.SKIPPED_MODEL_DATA_NUMBERS.toConfigSection() ?: return skippedData
            val skippedString = section.getString(type.name.lowercase()) ?: section.getString(type.name)

            skippedString?.let {
                if (".." in it) {
                    it.toIntRangeOrNull()?.forEach(skippedData::add) ?: Logs.logError("Invalid range for ${type.name} in settings.yml")
                } else {
                    it.toIntOrNull()?.let(skippedData::add) ?: Logs.logError("Invalid number for ${type.name} in settings.yml")
                }
            }

            section.getStringList(type.name.lowercase()).forEach { s ->
                if (".." in s) {
                    s.toIntRangeOrNull()?.forEach(skippedData::add) ?: Logs.logError("Invalid range for ${type.name} in settings.yml")
                } else {
                    s.toIntOrNull()?.let(skippedData::add) ?: Logs.logError("Invalid number for ${type.name} in settings.yml")
                }
            }

            return skippedData
        }
    }
}
