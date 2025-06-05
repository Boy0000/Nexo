package com.nexomc.nexo.utils

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
