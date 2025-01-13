package com.nexomc.nexo.converter

import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.logs.Logs
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File

object NexoConverter {

    fun processItemConfigs(itemFile: File) {
        if (itemFile.extension != "yml") return
        val itemLoader = YamlConfigurationLoader.builder().nodeStyle(NodeStyle.BLOCK).indent(2).file(itemFile).build()
        val itemYaml = runCatching { itemLoader.load() }.onFailure {
            Logs.logError("Failed to load ${itemFile.name} for parsing...")
            if (Settings.DEBUG.toBool()) it.printStackTrace()
        }.getOrNull() ?: return

        itemYaml.childrenMap().mapKeys { it.key.toString() }.forEach { (itemId, itemNode) ->
            if (itemNode.empty()) return@forEach

            runCatching {
                val furnitureNode = itemNode.node("Mechanics", "furniture").takeUnless { it.virtual() || it.empty() } ?: return@runCatching
                val lightsNode = furnitureNode.node("Lights").takeUnless { it.virtual() || it.empty() } ?: return@runCatching
                lightsNode.stringListOrNull?.apply {
                    lightsNode.node("lights").set(furnitureNode.removeChildNode("lights"))
                }
                furnitureNode.node("lights_model").renameNode(lightsNode.node("toggled_model"))
                furnitureNode.node("lights_item_model").renameNode(lightsNode.node("toggled_item_model"))
                furnitureNode.node("lights_toggleable").renameNode(lightsNode.node("toggleable"))

                if (lightsNode.empty()) furnitureNode.removeChildNode("lights")

            }.onFailure {
                Logs.logError("Failed to convert $itemId: ${it.message}")
                if (Settings.DEBUG.toBool()) it.printStackTrace()
            }

        }
        runCatching {
            itemLoader.save(itemYaml)
        }.onFailure {
            Logs.logWarn("Failed to save ${itemFile.name} changes...")
            if (Settings.DEBUG.toBool()) it.printStackTrace()
        }
    }
}