package com.nexomc.nexo.utils

import com.nexomc.nexo.configs.Settings
import kotlin.random.Random

inline fun <reified T> Any?.safeCast(): T? = this as? T
inline fun <reified T> Any?.ensureCast(): T = this as T

fun <T> Result<T>.printOnFailure(debugOnly: Boolean = false): Result<T> {
    if (!debugOnly || Settings.DEBUG.toBool()) exceptionOrNull()?.printStackTrace()
    return this
}

inline fun <reified T> Collection<T>.indexOfOrNull(element: T): Int? {
    return indexOf(element).takeIf { it >= 0 }
}

fun String.joinToString(separator: String) = this.toList().joinToString(separator)
fun String.removeSpaces() = replace(" ", "")
fun String.remove(remove: String) = replace(remove, "")
internal fun String.appendIfMissing(suffix: String) = if (endsWith(suffix)) this else (this + suffix)
internal fun String.prependIfMissing(prefix: String) = if (startsWith(prefix)) this else (prefix + this)
internal fun String.substringBetween(after: String, before: String) = this.substringAfter(after).substringBefore(before)
internal fun String.toIntRange(default: IntRange = IntRange.EMPTY): IntRange {
    val first = this.substringBefore("..").toIntOrNull() ?: default.first
    return first..(this.substringAfter("..").toIntOrNull() ?: default.last).coerceAtLeast(first)
}
internal fun String.toIntRangeOrNull(): IntRange? {
    val first = this.substringBefore("..").toIntOrNull() ?: return null
    val last = this.substringAfter("..").toIntOrNull()?.coerceAtLeast(first) ?: return null
    return first..last
}
internal fun String.toIntRangeOrDefault(start: Int, end: Int): IntRange {
    val first = this.substringBefore("..").toIntOrNull() ?: start
    val last = this.substringAfter("..").toIntOrNull()?.coerceAtLeast(first) ?: end
    return first..last
}

fun IntRange.randomOrMin(): Int =
    if (start >= endInclusive) start
    else Random.nextInt(start, endInclusive)

fun <T, Z> Map<T, Z?>.filterValuesNotNull() = filterValues { it != null }.ensureCast<Map<T, Z>>()
inline fun <K, V> Iterable<K>.associateWithNotNull(valueSelector: (K) -> V?): Map<K, V> {
    return associateWith(valueSelector).filterValuesNotNull()
}

inline fun <T> nonNullList(crossinline block: (List<T?>) -> T): List<T> {
    val result = mutableListOf<T>()
    block.invoke(result.toList())
    return result
}

val Float.radians: Float get() = Math.toRadians(this.toDouble()).toFloat()
