package com.nexomc.nexo.converter

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.NexoYaml
import com.nexomc.nexo.utils.childSections
import com.nexomc.nexo.utils.getStringListOrNull
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.rename
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import kotlin.io.path.pathString

object NexoConverter {

    fun processItemConfigs(configSection: ConfigurationSection) {
        configSection.childSections().forEach { itemId, section ->
            val furnitureSection = section.getConfigurationSection("Mechanics.furniture") ?: return@forEach

            runCatching {
                furnitureSection.getStringListOrNull("lights")?.apply {
                    furnitureSection.set("lights", null)
                    furnitureSection.set("lights.lights", this)
                }
                furnitureSection.rename("lights_model", "lights.toggled_model")
                furnitureSection.rename("lights_item_model", "lights.toggled_item_model")
                furnitureSection.rename("lights_toggleable", "lights.toggleable")

                furnitureSection.getConfigurationSection("properties.scale")?.let {
                    val default = if (furnitureSection.getString("properties.display_transform") == "FIXED") "1.0" else "0.5"
                    val scale = "${it.get("x", default)},${it.get("y", default)},${it.get("z", default)}"
                    furnitureSection.set("properties.scale", scale)
                }
            }.onFailure {
                Logs.logError("Failed to convert $itemId: ${it.message}")
                if (Settings.DEBUG.toBool()) it.printStackTrace()
            }
        }
    }

    fun processGlyphConfigs(glyphFile: File) {
        if (glyphFile.extension != "yml") return

        val glyphFolder = NexoPlugin.instance().dataFolder.toPath()
        val resourcePath = glyphFolder.relativize(glyphFile.toPath())
        val resource = NexoPlugin.instance().getResource(resourcePath.pathString) ?: return
        val resourceContent = YamlConfiguration().apply { loadFromString(resource.readAllBytes().decodeToString()) }

        val glyphConfig = NexoYaml.loadConfiguration(glyphFile)
        // Merge default glyphs with existing glyphs
        resourceContent.childSections().forEach { key, section ->
            if (glyphConfig.get(key) == null) glyphConfig.set(key, section)
        }

        glyphConfig.childSections().forEach { key, section ->
            val chatSection = section.getConfigurationSection("chat")
            if (chatSection != null) {
                NexoYaml.copyConfigurationSection(chatSection, section)
                section.set("chat", null)
            }
        }

        glyphConfig.save(glyphFile)
    }

    fun processSettings(settings: YamlConfiguration) {
        if (settings.getString(Settings.GLYPH_DEFAULT_PERMISSION.path)?.contains(":") == true)
            settings.set(Settings.GLYPH_DEFAULT_PERMISSION.path, null)
    }
}