package com.nexomc.nexo.utils

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

val Int.ticks: Duration get() = (this * 50).milliseconds
val Long.ticks: Duration get() = (this * 50).milliseconds

val Duration.inWholeTicks: Long get() = this.inWholeMilliseconds / 50

fun String.toDuration(): Duration? {
    val splitAt = indexOfFirst { it.isLetter() }.takeIf { it > 0 } ?: length
    val value = take(splitAt).toDouble()
    return when (drop(splitAt)) {
        "ms" -> value.milliseconds
        "t" -> value.toInt().ticks
        "s" -> value.seconds
        "m" -> value.minutes
        "h" -> value.hours
        "d" -> value.days
        "w" -> value.days * 7
        "mo" -> value.days * 31
        else -> null
    }
}
