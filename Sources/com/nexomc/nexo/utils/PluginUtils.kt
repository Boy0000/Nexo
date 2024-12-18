package com.nexomc.nexo.utils

import org.bukkit.Bukkit

object PluginUtils {
    fun isEnabled(pluginName: String) = Bukkit.getPluginManager().isPluginEnabled(pluginName)
}
