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
        if (!PluginUtils.isEnabled("WorldGuard")) return Bukkit.getLogger().warning("Failed to register NexoWorldGuardFlags: WorldGuard is not enabled")

        FURNITURE_INTERACT_FLAG = StateFlag("nexo-furniture-interact", false)
        FURNITURE_SIT_FLAG = StateFlag("nexo-furniture-sit", false)
        FURNITURE_TOGGLE_LIGHT_FLAG = StateFlag("nexo-furniture-toggle-light", false)
        FURNITURE_ROTATE_FLAG = StateFlag("nexo-furniture-rotate", false)
        FURNITURE_OPEN_STORAGE_FLAG = StateFlag("nexo-furniture-open-storage", false)
        FURNITURE_CLICKACTION_FLAG = StateFlag("nexo-furniture-run-actions", false)

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
