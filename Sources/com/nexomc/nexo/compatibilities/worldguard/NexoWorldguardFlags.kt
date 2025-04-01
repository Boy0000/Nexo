package com.nexomc.nexo.compatibilities.worldguard

import com.nexomc.nexo.utils.PluginUtils
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.flags.StateFlag
import org.bukkit.Bukkit

object NexoWorldguardFlags {
    var FURNITURE_INTERACT_FLAG: StateFlag? = null
    var FURNITURE_SIT_FLAG: StateFlag? = null
    var FURNITURE_TOGGLE_LIGHT_FLAG: StateFlag? = null
    var FURNITURE_ROTATE_FLAG: StateFlag? = null
    var FURNITURE_OPEN_STORAGE_FLAG : StateFlag? = null
    var FURNITURE_CLICKACTION_FLAG: StateFlag? = null

    fun registerFlags() {
        FURNITURE_INTERACT_FLAG = runCatching { StateFlag("nexo-furniture-interact", false) }.getOrNull() ?: return
        FURNITURE_SIT_FLAG = runCatching { StateFlag("nexo-furniture-sit", false) }.getOrNull() ?: return
        FURNITURE_TOGGLE_LIGHT_FLAG = runCatching { StateFlag("nexo-furniture-toggle-light", false) }.getOrNull() ?: return
        FURNITURE_ROTATE_FLAG = runCatching { StateFlag("nexo-furniture-rotate", false) }.getOrNull() ?: return
        FURNITURE_OPEN_STORAGE_FLAG = runCatching { StateFlag("nexo-furniture-open-storage", false) }.getOrNull() ?: return
        FURNITURE_CLICKACTION_FLAG = runCatching { StateFlag("nexo-furniture-run-actions", false) }.getOrNull() ?: return

        if (PluginUtils.isEnabled("WorldGuard")) {
            return Bukkit.getLogger().warning("Failed to register NexoWorldGuardFlags: WorldGuard is not enabled")
        }

        runCatching {
            val registry = WorldGuard.getInstance().flagRegistry
            registry.register(FURNITURE_INTERACT_FLAG)
            registry.register(FURNITURE_SIT_FLAG)
            registry.register(FURNITURE_TOGGLE_LIGHT_FLAG)
            registry.register(FURNITURE_ROTATE_FLAG)
            registry.register(FURNITURE_OPEN_STORAGE_FLAG)
            registry.register(FURNITURE_CLICKACTION_FLAG)
        }.onFailure { it.printStackTrace() }
    }
}
