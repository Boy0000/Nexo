package com.nexomc.nexo.utils

import com.nexomc.nexo.configs.Settings
import kotlin.random.Random

inline fun <reified T> Any?.safeCast(): T? = this as? T
inline fun <reified T> Any?.ensureCast(): T = this as T

fun <T> Result<T>.printOnFailure(debugOnly: Boolean = false): Result<T> {
    if (!debugOnly || Settings.DEBUG.toBool()) exceptionOrNull()?.printStackTrace()
    return this
}

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

fun IntRange.randomOrMin(): Int =
    if (start >= endInclusive) start
    else Random.nextInt(start, endInclusive)

fun <T, Z> Map<T, Z?>.filterValuesNotNull() = filterValues { it != null }.ensureCast<Map<T, Z>>()
inline fun <K, V> Iterable<K>.associateWithNotNull(valueSelector: (K) -> V?): Map<K, V> {
    return associateWith(valueSelector).filterValuesNotNull()
}