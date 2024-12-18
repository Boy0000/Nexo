package com.nexomc.nexo.fonts

import team.unnamed.creative.font.FontProvider
import team.unnamed.creative.font.SpaceFontProvider
import kotlin.math.abs

enum class Shift(private val unicode: String) {
    NULL(""),
    MINUS_1("\uE101"),
    MINUS_2("\uE102"),
    MINUS_4("\uE103"),
    MINUS_8("\uE104"),
    MINUS_16("\uE105"),
    MINUS_32("\uE106"),
    MINUS_64("\uE107"),
    MINUS_128("\uE108"),
    MINUS_256("\uE109"),
    MINUS_512("\uE110"),

    PLUS_1("\uE112"),
    PLUS_2("\uE113"),
    PLUS_4("\uE114"),
    PLUS_8("\uE115"),
    PLUS_16("\uE116"),
    PLUS_32("\uE117"),
    PLUS_64("\uE118"),
    PLUS_128("\uE119"),
    PLUS_256("\uE120"),
    PLUS_512("\uE121");

    fun toNumber(): Int {
        return when (name.substringBefore("_")) {
            "PLUS" -> name.substringAfter("_").toIntOrNull() ?: 0
            "MINUS" -> -(name.substringAfter("_").toIntOrNull() ?: 0)
            else -> 0
        }
    }

    override fun toString(): String {
        return this.unicode
    }

    companion object {
        private val powers_plus = listOf(
            NULL, PLUS_1, PLUS_2, PLUS_4,
            PLUS_8, PLUS_16, PLUS_32, PLUS_64,
            PLUS_128, PLUS_256, PLUS_512
        )

        private val powers_minus = listOf(
            NULL, MINUS_1, MINUS_2, MINUS_4,
            MINUS_8, MINUS_16, MINUS_32, MINUS_64,
            MINUS_128, MINUS_256, MINUS_512
        )

        fun of(shift: Int) = buildString {
            val powers = if (shift > 0) powers_plus else powers_minus
            repeat(powers.size) { i ->
                val pow = i + 1
                val bit = 1 shl (i)
                if (abs(shift) and bit != 0)
                    append(powers[pow].unicode)
            }
        }.reversed()

        val fontProvider: SpaceFontProvider = FontProvider.space(Shift.entries.asSequence().filterNot(NULL::equals).associate { it.unicode to it.toNumber() })
    }
}
