package com.nexomc.nexo.commands

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.ConfigsManager
import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.utils.AdventureUtils.tagResolver
import com.nexomc.nexo.utils.printOnFailure
import com.nexomc.protectionlib.ProtectionLib
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.literalArgument

internal fun CommandTree.debugCommand() = literalArgument("debug") {
    withPermission("nexo.command.debug")
    anyExecutor { sender, _ ->
        val settings = NexoPlugin.instance().configsManager().settings()
        val debugState = !settings.getBoolean("debug", true)
        settings.set("debug", debugState)
        runCatching {
            settings.save(ConfigsManager.settingsFile)
            ProtectionLib.debug = debugState
            Message.DEBUG_TOGGLE.send(sender, tagResolver("state", if (debugState) "enabled" else "disabled"))
        }.printOnFailure()
    }
}
