package com.nexomc.nexo.nms

import org.bukkit.entity.Player

interface Transformer<T> {
    fun transform(input: T): T
}

abstract class AbstractTransformer<T>(protected val player: Player) : Transformer<T>
