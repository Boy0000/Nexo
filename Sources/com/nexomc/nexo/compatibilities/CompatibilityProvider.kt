package com.nexomc.nexo.compatibilities

import com.nexomc.nexo.NexoPlugin
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

open class CompatibilityProvider<T : Plugin> : Listener {
    private var pluginName: String? = null
    protected var plugin: T? = null
    var isEnabled = false

    fun enable(pluginName: String) {
        Bukkit.getPluginManager().registerEvents(this, NexoPlugin.instance())
        this.isEnabled = true
        this.pluginName = pluginName
        runCatching {
            this.plugin = Bukkit.getPluginManager().getPlugin(pluginName) as? T
        }
    }

    open fun disable() {
        HandlerList.unregisterAll(this)
        this.isEnabled = false
    }

    fun pluginName() = pluginName

    fun plugin() = plugin
}
