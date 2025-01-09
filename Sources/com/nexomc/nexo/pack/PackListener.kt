package com.nexomc.nexo.pack

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.VersionUtil
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class PackListener : Listener {
    @EventHandler(priority = EventPriority.NORMAL)
    fun PlayerJoinEvent.onPlayerConnect() {
        if (!Settings.PACK_SEND_ON_JOIN.toBool()) return
        if (Settings.PACK_SEND_PRE_JOIN.toBool() && (VersionUtil.atleast("1.21") || !VersionUtil.isPaperServer || player.hasResourcePack())) return

        val delay = Settings.PACK_SEND_DELAY.toInt(-1)
        if (delay <= 0) NexoPlugin.instance().packServer().sendPack(player)
        else Bukkit.getScheduler().runTaskLaterAsynchronously(
            NexoPlugin.instance(),
            Runnable { NexoPlugin.instance().packServer().sendPack(player) }, delay * 20L
        )
    }
}
