package com.nexomc.nexo.utils

import com.mineinabyss.idofront.util.Quadruple
import com.nexomc.nexo.configs.Settings
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.bukkit.Color
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import java.io.Serializable
import java.nio.file.FileSystems
import java.util.*

inline fun <reified T> Any?.safeCast(): T? = this as? T
inline fun <reified T> Any?.ensureCast(): T = this as T

fun <T> Result<T>.printOnFailure(debugOnly: Boolean = false): Result<T> {
    if (!debugOnly || Settings.DEBUG.toBool()) exceptionOrNull()?.printStackTrace()
    return this
}

inline fun <T, R> Iterable<T>.mapFast(transform: (T) -> R): ObjectArrayList<R> {
    return mapTo(ObjectArrayList<R>((this as? Collection)?.size ?: 10), transform)
}

inline fun <T, R> Iterable<T>.mapFastSet(transform: (T) -> R): ObjectOpenHashSet<R> {
    return mapTo(ObjectOpenHashSet<R>((this as? Collection)?.size ?: 10), transform)
}

inline fun <T, K, V> Iterable<T>.associateFast(transform: (T) -> Pair<K, V>): Object2ObjectOpenHashMap<K, V> {
    val capacity = mapCapacity((this as? Collection)?.size ?: 10).coerceAtLeast(16)
    return associateTo(Object2ObjectOpenHashMap<K, V>(capacity), transform)
}

inline fun <reified R> Iterable<*>.filterFastIsInstance(): ObjectArrayList<R> {
    return filterIsInstanceTo(ObjectArrayList<R>())
}

inline fun <reified R> Iterable<*>.filterFastIsInstance(predicate: (R) -> Boolean): ObjectArrayList<R> {
    val result = ObjectArrayList<R>()
    for (element in this) if (element is R && predicate(element)) result.add(element)
    return result
}

inline fun <T> Iterable<T>.filterFast(predicate: (T) -> Boolean): ObjectArrayList<T> {
    return filterTo(ObjectArrayList<T>(), predicate)
}

inline fun <T> Iterable<T>.filterFastSet(predicate: (T) -> Boolean): ObjectOpenHashSet<T> {
    return filterTo(ObjectOpenHashSet<T>(), predicate)
}

fun <K, V> Iterable<Pair<K, V>>.toFastMap(): Object2ObjectOpenHashMap<K, V> {
    if (this is Collection) {
        return when (size) {
            0 -> Object2ObjectOpenHashMap()
            1 -> fastMapOf(if (this is List) this[0] else iterator().next())
            else -> toMap(Object2ObjectOpenHashMap<K, V>(mapCapacity(size)))
        }
    }
    return toMap(LinkedHashMap<K, V>()).optimizeReadOnlyMap()
}

fun <K, V> fastMapOf(pair: Pair<K, V>): Object2ObjectOpenHashMap<K, V> = Object2ObjectOpenHashMap<K, V>().apply { put(pair.first, pair.second) }

fun mapCapacity(expectedSize: Int): Int = when {
    // We are not coercing the value to a valid one and not throwing an exception. It is up to the caller to
    // properly handle negative values.
    expectedSize < 0 -> expectedSize
    expectedSize < 3 -> expectedSize + 1
    expectedSize < INT_MAX_POWER_OF_TWO -> ((expectedSize / 0.75F) + 1.0F).toInt()
    // any large value
    else -> Int.MAX_VALUE
}

internal fun <K, V> Map<K, V>.optimizeReadOnlyMap() = when (size) {
    0 -> Object2ObjectOpenHashMap()
    1 -> Object2ObjectOpenHashMap(with(entries.iterator().next()) { Collections.singletonMap(key, value) })
    else -> Object2ObjectOpenHashMap(this)
}

private const val INT_MAX_POWER_OF_TWO: Int = 1 shl (Int.SIZE_BITS - 2)

internal fun String.appendIfMissing(suffix: String) = if (endsWith(suffix)) this else (this + suffix)
internal fun String.prependIfMissing(prefix: String) = if (startsWith(prefix)) this else (prefix + this)
internal fun String.substringBetween(after: String, before: String) = this.substringAfter(after).substringBefore(before)
internal fun String.toIntRange(default: IntRange = IntRange.EMPTY): IntRange {
    val first = this.substringBefore("..").toIntOrNull() ?: default.first
    return first..(this.substringAfter("..").toIntOrNull() ?: default.last).coerceAtLeast(first)
}
internal fun String.toIntRangeOrNull(): IntRange? {
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
