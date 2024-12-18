package com.nexomc.nexo.utils.libs

import com.nexomc.nexo.NexoPlugin
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig

class CommandAPIManager(val plugin: NexoPlugin) {
    fun load() {
        CommandAPI.onLoad(CommandAPIBukkitConfig(plugin).silentLogs(true).skipReloadDatapacks(true))
    }

    fun enable() {
        CommandAPI.onEnable()
    }

    fun disable() {
        CommandAPI.onDisable()
    }
}