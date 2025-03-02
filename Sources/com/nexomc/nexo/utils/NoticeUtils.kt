package com.nexomc.nexo.utils

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.utils.logs.Logs
import org.bukkit.Bukkit

object NoticeUtils {
    fun compileNotice() {
        Logs.logError("This is a compiled version of Nexo.")
        Logs.logWarn("Compiled versions come without Default assets and support is not provided.")
        Logs.logWarn("Consider purchasing Nexo on MCModels or Polymart for access to the full version.")
    }

    fun ciNotice() {
        Logs.logError("This is a CI version of Nexo.")
        Logs.logWarn("CI versions are not supported and are not guaranteed to work.")
        Logs.logWarn("Consider purchasing Nexo on MCModels or Polymart for access to the full version.")
        Bukkit.getPluginManager().disablePlugin(NexoPlugin.instance())
    }

    fun leakNotice() {
        Logs.logError("This is a leaked version of Nexo")
        Logs.logError("Piracy is not supported, shutting down plugin.")
        Logs.logError("Consider purchasing Nexo on MCModels or Polymart if you want a working version.")
        Bukkit.getPluginManager().disablePlugin(NexoPlugin.instance())
    }

    fun failedLibs() {
        Logs.logError("Nexo failed to download/load certain libraries...")
        Logs.logError("This will cause some functionalities not to work")
        Logs.logError("Please restart your server, if this continues download NexoLibs aswell")
    }
}
