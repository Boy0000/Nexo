package com.nexomc.nexo.converter

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.NexoYaml
import com.nexomc.nexo.utils.childSections
import com.nexomc.nexo.utils.logs.Logs
import org.bukkit.configuration.file.YamlConfiguration
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import kotlin.io.path.pathString

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
                val lightsNode = furnitureNode.node("lights").takeUnless { it.virtual() || it.empty() } ?: return@runCatching
                lightsNode.stringListOrNull?.apply {
                    lightsNode.node("lights").set(furnitureNode.removeChildNode("lights"))
                }
                furnitureNode.node("lights_model").renameNode(lightsNode.node("toggled_model"))
                furnitureNode.node("lights_item_model").renameNode(lightsNode.node("toggled_item_model"))
                furnitureNode.node("lights_toggleable").renameNode(lightsNode.node("toggleable"))

                if (lightsNode.empty()) furnitureNode.removeChildNode("lights")

                furnitureNode.node("properties", "scale").takeIf { it.isMap }?.ifExists {
                    val default = if (furnitureNode.node("properties", "display_transform").getString("NONE") == "FIXED") "1.0" else "0.5"
                    it.set(it.childrenMap().let { m -> "${m["x"]?.raw() ?: default},${m["y"]?.raw() ?: default},${m["z"]?.raw() ?: default}" })
                }
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

    fun processGlyphConfigs(glyphFile: File) {
        if (glyphFile.extension != "yml") return

        val glyphFolder = NexoPlugin.instance().dataFolder.toPath()
        val resourcePath = glyphFolder.relativize(glyphFile.toPath())
        val resource = NexoPlugin.instance().getResource(resourcePath.pathString) ?: return
        val resourceContent = YamlConfiguration().apply { loadFromString(resource.readAllBytes().decodeToString()) }

        val glyphConfig = NexoYaml.loadConfiguration(glyphFile)
        resourceContent.childSections().forEach { key, section ->
            if (glyphConfig.get(key) == null) glyphConfig.set(key, section)
        }
        glyphConfig.save(glyphFile)
    }
}