package com.nexomc.nexo.mechanics.furniture.compatibility

import com.nexomc.nexo.mechanics.furniture.BlockLocation
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager
import com.nexomc.nexo.utils.NexoYaml
import com.nexomc.nexo.utils.logs.Logs
import me.frep.vulcan.api.VulcanAPI
import me.frep.vulcan.api.event.VulcanFlagEvent
import me.frep.vulcan.api.event.VulcanGhostBlockEvent
import me.frep.vulcan.api.event.VulcanPunishEvent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class VulcanCompatibility : Listener {

    init {
        Logs.logSuccess("Initializing Vulcan-AntiCheat Hook!")
        val isApiEnabled = Bukkit.getPluginManager().getPlugin("Vulcan")?.dataFolder?.resolve("config.yml")
            ?.let(NexoYaml::loadConfiguration)?.getBoolean("settings.enable-api", false) ?: false
        if (!isApiEnabled) Logs.logWarn("Vulcan's API is disabled, please enable <i>settings.enable-api</i>...")
    }

    @EventHandler
    fun VulcanFlagEvent.onFurnitureBarrier() {
        Logs.logError("${player.name} ghost block at $timestamp for $check")
    }

    @EventHandler
    fun VulcanGhostBlockEvent.onFurnitureBarrier() {
        if (IFurniturePacketManager.standingOnFurniture(player)) isCancelled = true
    }

    @EventHandler
    fun VulcanPunishEvent.onPunish() {
        Logs.logWarn("punished ${player.name} for $check")
    }
}