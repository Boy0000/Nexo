package com.nexomc.nexo.utils.timers

import org.bukkit.entity.Player
import java.util.*

class TimersFactory(val delay: Long) {
    constructor(delay: Int) : this(delay.toLong())

    private val timersPerUUID = mutableMapOf<UUID, Timer>()
    fun getTimer(player: Player) = timersPerUUID.computeIfAbsent(player.uniqueId) { Timer(delay) }
}
