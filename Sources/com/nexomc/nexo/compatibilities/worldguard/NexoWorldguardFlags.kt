package com.nexomc.nexo.compatibilities.worldguard

import com.nexomc.nexo.utils.PluginUtils
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.flags.StateFlag
import org.bukkit.Bukkit

object NexoWorldguardFlags {
    var FURNITURE_INTERACT_FLAG: StateFlag? = null
        private set

    fun registerFlags() {
        if (FURNITURE_INTERACT_FLAG == null) {
            FURNITURE_INTERACT_FLAG = runCatching {
                StateFlag("nexo-furniture-interact", false)
            }.getOrNull() ?: return
        }

        if (PluginUtils.isEnabled("WorldGuard")) {
            return Bukkit.getLogger().warning("Failed to register NexoWorldGuardFlags: WorldGuard is not enabled")
        }

        runCatching {
            WorldGuard.getInstance().flagRegistry.register(FURNITURE_INTERACT_FLAG)
        }.onFailure { it.printStackTrace() }
    }
}
