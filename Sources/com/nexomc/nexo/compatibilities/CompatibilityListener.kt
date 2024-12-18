package com.nexomc.nexo.compatibilities

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.event.server.PluginEnableEvent

class CompatibilityListener : Listener {
    @EventHandler
    fun PluginEnableEvent.onPluginEnable() {
        CompatibilitiesManager.enableCompatibility(plugin.name)
    }

    @EventHandler
    fun PluginDisableEvent.onPluginDisable() {
        CompatibilitiesManager.disableCompatibility(plugin.name)
    }
}
