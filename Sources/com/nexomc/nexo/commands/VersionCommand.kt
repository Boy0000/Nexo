package com.nexomc.nexo.commands

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.utils.AdventureUtils.tagResolver
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.literalArgument

internal fun CommandTree.versionCommand() = literalArgument("version") {
    withPermission("nexo.command.version")
    anyExecutor { sender, _ ->
        Message.VERSION.send(sender, tagResolver("version", NexoPlugin.instance().description.version))
    }
}

object VersionCommand
