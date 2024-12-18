package com.nexomc.nexo.utils

import java.util.*

enum class InteractionResult {
    SUCCESS,
    CONSUME,
    CONSUME_PARTIAL,
    PASS,
    FAIL;

    fun consumesAction() = this == SUCCESS || this == CONSUME || this == CONSUME_PARTIAL
    fun shouldSwing() = this == SUCCESS

    fun shouldAwardStats() = this == SUCCESS || this == CONSUME

    companion object {
        fun sidedSuccess(swingHand: Boolean) = if (swingHand) SUCCESS else CONSUME
        fun fromNms(nmsEnum: Enum<*>) = valueOf(nmsEnum.name)
        fun fromNms(nmsClass: Class<*>) = valueOf(nmsClass.name.uppercase(Locale.getDefault()).substringAfter("$"))
    }
}
