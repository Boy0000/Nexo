package com.nexomc.nexo.nms

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.logs.Logs
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList


object NMSHandlers {
    private val SUPPORTED_VERSION = VersionUtil.NMSVersion.entries.toTypedArray()
    private var handler: NMSHandler = setupHandler()
    var version: String? = null

    @JvmStatic
    fun handler() = handler

    @JvmStatic
    fun resetHandler() {
        HandlerList.unregisterAll(handler.packDispatchListener)
        setupHandler()
    }

    private fun setupHandler(): NMSHandler {
        handler = EmptyNMSHandler()
        for (selectedVersion in SUPPORTED_VERSION) {
            if (!VersionUtil.matchesServer(selectedVersion)) continue

            version = selectedVersion.name
            runCatching {
                handler = Class.forName("com.nexomc.nexo.nms.$version.NMSHandler").getConstructor().newInstance() as NMSHandler
                Logs.logSuccess("NMS-Version $version has been detected.")
                Bukkit.getPluginManager().registerEvents(handler.packDispatchListener, NexoPlugin.instance())
                return handler
            }.onFailure {
                if (Settings.DEBUG.toBool()) it.printStackTrace()
                Logs.logWarn("Nexo does not support this version of Minecraft ($version) yet.")
                Logs.logWarn("NMS features will be disabled...", true)
                handler = EmptyNMSHandler()
            }
        }

        return handler
    }
}
