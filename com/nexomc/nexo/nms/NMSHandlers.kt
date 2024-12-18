package com.nexomc.nexo.nms

import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.VersionUtil.NMSVersion.Companion.matchesServer
import com.nexomc.nexo.utils.logs.Logs
import org.bukkit.event.HandlerList


object NMSHandlers {
    private val SUPPORTED_VERSION = VersionUtil.NMSVersion.entries.toTypedArray()
    private var handler: NMSHandler = setupHandler()
    var version: String? = null

    @JvmStatic
    fun handler() = handler

    @JvmStatic
    fun resetHandler() {
        handler.resourcePackListener?.also { HandlerList.unregisterAll(it) }
        handler = EmptyNMSHandler()
        setupHandler()
    }

    private fun setupHandler(): NMSHandler {
        handler = EmptyNMSHandler()
        SUPPORTED_VERSION.forEach { selectedVersion ->
            if (!matchesServer(selectedVersion)) return@forEach

            version = selectedVersion.name
            runCatching {
                handler = Class.forName("com.nexomc.nexo.nms.$version.NMSHandler").getConstructor().newInstance() as NMSHandler
                Logs.logSuccess("Version $version has been detected.")
                Logs.logInfo("Nexo will use the NMSHandler for this version.", true)
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
