package com.nexomc.nexo.dialog

import com.nexomc.nexo.utils.childSections
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput.OptionEntry
import io.papermc.paper.registry.data.dialog.input.TextDialogInput
import net.kyori.adventure.text.Component
import org.bukkit.configuration.ConfigurationSection
import kotlin.math.min

data class NexoDialogInput(val key: String, val inputSection: ConfigurationSection) {

    fun createDialogInput(): DialogInput? {
        val type = runCatching { DialogInputTypes.valueOf(inputSection.getString("type")!!) }.getOrDefault(DialogInputTypes.TEXT)

        return when (type) {
            DialogInputTypes.TEXT -> {
                val width = inputSection.getInt("width", 200).coerceIn(1..1024)
                val label = inputSection.getRichMessage("label") ?: Component.empty()
                val labelVisible = inputSection.getBoolean("labelVisible", true)
                val maxLength = inputSection.getInt("maxLength", 32).coerceAtLeast(0)
                val initial = inputSection.getString("initial", "")!!.take(maxLength)
                val multilineOptions = multilineOptions()

                DialogInput.text(key, width, label, labelVisible, initial, maxLength, multilineOptions)
            }
            DialogInputTypes.BOOL -> {
                val label = inputSection.getRichMessage("label") ?: Component.empty()
                val initial = inputSection.getBoolean("initial")
                val onTrue = inputSection.getString("onTrue") ?: ""
                val onFalse = inputSection.getString("onFalse") ?: ""

                DialogInput.bool(key, label, initial, onTrue, onFalse)
            }
            DialogInputTypes.NUMBER -> {
                val width = inputSection.getInt("width", 200).coerceIn(1..1024)
                val label = inputSection.getRichMessage("label") ?: Component.empty()
                val labelFormat = inputSection.getString("labelFormat") ?: "options.generic_value"
                val start = inputSection.getDouble("start", 0.0).toFloat()
                val end = inputSection.getDouble("end", 1.0).toFloat()
                val initial = if ("initial" in inputSection) inputSection.getDouble("initial", end / 2.0).coerceAtLeast(0.0).toFloat() else null
                val step = inputSection.getDouble("step").takeIf { it > 0.0 }?.toFloat()

                DialogInput.numberRange(key, width, label, labelFormat, start, end, initial, step)
            }
            DialogInputTypes.SINGLE -> {
                val width = inputSection.getInt("width", 200).coerceIn(1..1024)
                val label = inputSection.getRichMessage("label") ?: Component.empty()
                val labelVisible = inputSection.getBoolean("labelVisible", true)
                val optionEntries = inputSection.getConfigurationSection("options")?.childSections()?.values?.mapNotNull(::singleOptionEntry)

                if (optionEntries.isNullOrEmpty()) {
                    NexoDialogs.context.logger.warn("Skipped ${inputSection.currentPath} as there was no valid options specified.")
                    return null
                }

                DialogInput.singleOption(key, width, optionEntries, label, labelVisible)
            }
        }
    }

    private fun multilineOptions(): TextDialogInput.MultilineOptions? {
        val multilineSection = inputSection.getConfigurationSection("multilineOptions") ?: return null
        val maxLines = if ("maxLines" in multilineSection) multilineSection.getInt("maxLines").coerceAtLeast(0) else null
        val height = if ("height" in multilineSection) min(multilineSection.getInt("height"), 512).coerceIn(1..512) else null

        return TextDialogInput.MultilineOptions.create(maxLines, height)
    }

    private fun singleOptionEntry(option: ConfigurationSection): OptionEntry? {
        val display = option.getRichMessage("display", Component.text(option.name))
        val initial = option.getBoolean("initial")

        return OptionEntry.create(option.name, display, initial)
    }
}