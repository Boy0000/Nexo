package com.nexomc.nexo.commands

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.converter.OraxenConverter
import com.nexomc.nexo.utils.logs.Logs
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.literalArgument

fun CommandTree.convertCommand() = literalArgument("convert") {
    withPermission("nexo.command.convert")
    literalArgument("oraxen") {
        anyExecutor { _, _ ->
            val converter = NexoPlugin.instance().converter().oraxenConverter
            if (converter.convertItems) NexoPlugin.instance().dataFolder.resolve("items").walkBottomUp().forEach(OraxenConverter::processItemConfigs)
            if (converter.convertResourcePack) OraxenConverter.processPackFolder(NexoPlugin.instance().dataFolder.resolve("pack"))
            NexoPlugin.instance().dataFolder.resolve("recipes").listFiles { file -> file.extension == "yml" }?.forEach(OraxenConverter::processRecipes)
            Logs.logSuccess("Finished converting items, resourcepack & recipes!")
        }
    }
}