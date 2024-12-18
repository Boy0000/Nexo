package com.nexomc.nexo.utils.timers

import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.utils.AdventureUtils.tagResolver
import org.bukkit.entity.Player

class Timer(private val delay: Long, private var lastUsage: Long = 0) {

    fun reset() {
        lastUsage = System.currentTimeMillis()
    }

    val isFinished get() = System.currentTimeMillis() >= lastUsage + delay

    val remainingTime get() = lastUsage + delay - System.currentTimeMillis()

    val string get() = "%.2f".format(remainingTime / 1000f)

    fun sendToPlayer(player: Player?) {
        Message.COOLDOWN.send(player, tagResolver("time", string))
    }
}
