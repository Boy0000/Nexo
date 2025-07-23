package com.nexomc.nexo.utils

import org.bukkit.entity.Player

inline fun Player.withOp(block: () -> Unit) {
    val originalOp = isOp
    isOp = true
    runCatching(block)
    isOp = originalOp
}

object PlayerUtils