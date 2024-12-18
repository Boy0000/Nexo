package com.nexomc.nexo.utils

import com.nexomc.nexo.configs.Settings
import org.bukkit.Color
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import java.nio.file.FileSystems

inline fun <reified T> Any?.safeCast(): T? = this as? T
inline fun <reified T> Any?.ensureCast(): T = this as T

fun <T> Result<T>.printOnFailure(debugOnly: Boolean = false): Result<T> {
    if (!debugOnly || Settings.DEBUG.toBool()) exceptionOrNull()?.printStackTrace()
    return this
}

fun String.appendIfMissing(suffix: String) = if (endsWith(suffix)) this else (this + suffix)
fun String.prependIfMissing(prefix: String) = if (startsWith(prefix)) this else (prefix + this)
fun String.substringBetween(after: String, before: String) = this.substringAfter(after).substringBefore(before)
fun String.toIntRange(default: IntRange = IntRange.EMPTY): IntRange {
    val first = this.substringBefore("..").toIntOrNull() ?: default.first
    return first..(this.substringAfter("..").toIntOrNull() ?: default.last).coerceAtLeast(first)
}
fun String.toIntRangeOrNull(): IntRange? {
    val first = this.substringBefore("..").toIntOrNull() ?: return null
    return first..(this.substringAfter("..").toIntOrNull() ?: return null)
}

object Utils {

    /**
     * Removes extension AND parent directories
     * @param s The path or filename including extension
     * @return Purely the filename, no extension or path
     */
    @JvmStatic
    fun removeExtension(s: String): String {
        val separator = FileSystems.getDefault().separator
        val filename: String

        // Remove the path upto the filename.
        val lastSeparatorIndex = s.lastIndexOf(separator)
        filename = if (lastSeparatorIndex == -1) s
        else s.substring(lastSeparatorIndex + 1)

        // Remove the extension.
        return removeExtensionOnly(filename)
    }

    fun removeExtensionOnly(s: String): String {
        // Remove the extension.
        val extensionIndex = s.lastIndexOf(".")
        if (extensionIndex == -1) return s

        return s.substring(0, extensionIndex)
    }

    fun getStringBeforeLastInSplit(string: String, split: String): String {
        val splitString = string.split(split.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return if (splitString.isEmpty()) string
        else string.replace(splitString[splitString.size - 1], "")
    }

    fun firstEmptyChar(map: Map<String, Char>, min: Int = 42000): Char {
        var min = min
        val newMap = map.values.map(Char::code).sorted().toList()
        while (min in newMap) min++
        return min.toChar()
    }

    fun swingHand(player: Player, hand: EquipmentSlot) {
        if (hand == EquipmentSlot.HAND) player.swingMainHand()
        else player.swingOffHand()
    }
}
