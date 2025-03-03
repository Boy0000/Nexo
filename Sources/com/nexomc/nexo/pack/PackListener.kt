package com.nexomc.nexo.pack

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.SchedulerUtils
import com.nexomc.nexo.utils.VersionUtil
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class PackListener : Listener {
    @EventHandler(priority = EventPriority.NORMAL)
    fun PlayerJoinEvent.onPlayerConnect() {
        // If both are false, return early
        if (!Settings.PACK_SEND_ON_JOIN.toBool() && !Settings.PACK_SEND_PRE_JOIN.toBool()) return

        // If on-join is false and pre-join is true, check sub-conditions
        if (!Settings.PACK_SEND_ON_JOIN.toBool() && Settings.PACK_SEND_PRE_JOIN.toBool()) {
            if (VersionUtil.atleast("1.21") && player.hasResourcePack()) return
        }

        val delay = Settings.PACK_SEND_DELAY.toInt(-1)
        if (delay <= 0) NexoPlugin.instance().packServer().sendPack(player)
        else SchedulerUtils.foliaScheduler.runAtEntityLater(
            player, Runnable { NexoPlugin.instance().packServer().sendPack(player) }, delay * 20L
        )
    }

}
