package com.nexomc.nexo.glyphs

import com.nexomc.nexo.utils.filterFast
import com.nexomc.nexo.utils.logs.Logs
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import team.unnamed.creative.font.FontProvider
import team.unnamed.creative.font.SpaceFontProvider
import kotlin.math.abs

object Shift {
    val defaultShiftProvider: SpaceFontProvider = FontProvider.space(
        mapOf(
            "\uE101" to -1, "\uE102" to -2, "\uE103" to -4, "\uE104" to -8, "\uE105" to -16,
            "\uE106" to -32, "\uE107" to -64, "\uE108" to -128, "\uE109" to -256, "\uE110" to -512,

            "\uE112" to 1, "\uE113" to 2, "\uE114" to 4, "\uE115" to 8, "\uE116" to 16,
            "\uE117" to 32, "\uE118" to 64, "\uE119" to 128, "\uE120" to 256, "\uE121" to 512
        )
    )

    var fontProvider: SpaceFontProvider = defaultShiftProvider
        get() = field.takeUnless {
            it.advances().isEmpty()
        } ?: run {
            Logs.logWarn("Provided Shift-FontProvider was empty, defaulting to Nexo's built-in")
            fontProvider = defaultShiftProvider
            defaultShiftProvider
        }
        set(value) {
            field = value.takeIf { it.advances().isNotEmpty() } ?: defaultShiftProvider
            // Update cache whenever fontProvider is changed
            cachedPowers.clear()
            cachedPowers.addAll(field.cachedPowers())
        }

    private val cachedPowers: ObjectArrayList<Pair<String, Int>> = ObjectArrayList(defaultShiftProvider.cachedPowers())

    private fun SpaceFontProvider.cachedPowers() = advances().toList().sortedByDescending { abs(it.second) }.filterFast { it.second != 0 }

    fun of(shift: Int): String {
        var remainingShift = abs(shift)
        return buildString {
            for ((char, value) in cachedPowers) {
                if (shift < 0 && value > 0) continue // Skip positive values for negative shifts
                if (shift > 0 && value < 0) continue // Skip negative values for positive shifts

                val absValue = abs(value)
                if (remainingShift >= absValue) {
                    append(char)
                    remainingShift -= absValue
                }
            }
        }
    }
}
