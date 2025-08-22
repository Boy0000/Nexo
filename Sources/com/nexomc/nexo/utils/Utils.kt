package com.nexomc.nexo.utils

import com.nexomc.nexo.configs.Settings
import kotlin.random.Random

inline fun <reified T> Any?.safeCast(): T? = this as? T
inline fun <reified T> Any?.ensureCast(): T = this as T

fun <T> Result<T>.printOnFailure(debugOnly: Boolean = false): Result<T> {
    if (!debugOnly || Settings.DEBUG.toBool()) exceptionOrNull()?.printStackTrace()
    return this
}

inline fun <T> T.applyIf(condition: Boolean, transform: T.() -> T): T =
    if (condition) transform() else this

inline fun <T> T.applyAt(condition: Boolean, transform: T.() -> Unit): T {
    if (condition) transform()
    return this
}


inline fun <reified T> Collection<T>.indexOfOrNull(element: T): Int? {
    return indexOf(element).takeIf { it >= 0 }
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


inline fun <reified V> Map<String, *>.filterValuesInstanceOf(): Map<String, V> {
    return mapNotNull { (key, value) -> if (value is V) key to value else null }.toMap()
}

fun <T, Z> Map<T, Z?>.filterValuesNotNull() = filterValues { it != null }.ensureCast<Map<T, Z>>()
inline fun <K, V> Iterable<K>.associateWithNotNull(valueSelector: (K) -> V?): Map<K, V> {
    return associateWith(valueSelector).filterValuesNotNull()
}
inline fun <K, V> Array<K>.associateWithNotNull(valueSelector: (K) -> V?): Map<K, V> {
    return associateWith(valueSelector).filterValuesNotNull()
}

inline fun <T> nonNullList(crossinline block: (List<T?>) -> T): List<T> {
    val result = mutableListOf<T>()
    block.invoke(result.toList())
    return result
}

val Float.radians: Float get() = Math.toRadians(this.toDouble()).toFloat()
