package com.nexomc.nexo.converter

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.recipes.RecipeType
import com.nexomc.nexo.recipes.RecipesManager
import com.nexomc.nexo.utils.NexoYaml
import com.nexomc.nexo.utils.childSections
import com.nexomc.nexo.utils.copyFrom
import com.nexomc.nexo.utils.getStringListOrNull
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.moveTo
import com.nexomc.nexo.utils.remove
import com.nexomc.nexo.utils.rename
import com.nexomc.nexo.utils.resolve
import com.nexomc.nexo.utils.toMap
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists
import kotlin.io.path.moveTo
import kotlin.io.path.pathString

object NexoConverter {

    fun processItemConfigs(configSection: ConfigurationSection) {
        configSection.childSections().forEach { itemId, section ->
            if (section.getBoolean("template")) section.remove("template")

            section.getConfigurationSection("Components.durability")?.let { durability ->
                durability.parent!!.set(durability.name, durability.getInt("value"))
            }
            section.rename("Components.equippable.model", "Components.equippable.asset_id")

            runCatching {
                val furnitureSection = section.getConfigurationSection("Mechanics.furniture") ?: return@forEach
                furnitureSection.rename("display_entity_properties", "properties")
                furnitureSection.rename("hitbox.barrierHitboxes", "hitbox.barriers")
                furnitureSection.remove("type")
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
        val glyphConfig = NexoYaml.loadConfiguration(glyphFile)


        // Merge default glyphs with existing glyphs
        val resourcePath = NexoPlugin.instance().dataFolder.toPath().relativize(glyphFile.toPath())
        val resource = NexoPlugin.instance().getResource(resourcePath.pathString)
        YamlConfiguration().apply { resource?.readAllBytes()?.decodeToString()?.let { loadFromString(it) } }
            .childSections().forEach { key, section ->
                val char = glyphConfig.get("$key.char")
                glyphConfig.set(key, section)
                glyphConfig.set("$key.char", char)
            }

        glyphConfig.childSections().values.forEach { section ->
            section.getConfigurationSection("chat")?.moveTo(section)
        }

        glyphConfig.save(glyphFile)
    }

    fun processRecipes() {
        RecipeType.entries.forEach {
            val recipeFile = RecipesManager.recipesFolder.resolve(it.id + ".yml").toPath().takeIf { it.exists() } ?: return@forEach
            val targetFile = RecipesManager.recipesFolder.resolve(it.id).apply { mkdirs() }.resolve(it.id + ".yml")
            if (targetFile.exists()) {
                val newRecipe = NexoYaml.loadConfiguration(targetFile).copyFrom(NexoYaml.loadConfiguration(recipeFile.toFile()))
                println(newRecipe.toMap().toString())
                NexoYaml.saveConfig(targetFile, newRecipe)
                recipeFile.deleteExisting()
            } else recipeFile.moveTo(targetFile.toPath(), true)
        }
    }

    fun processSettings(settings: YamlConfiguration) {
        if (settings.getString(Settings.GLYPH_DEFAULT_PERMISSION.path)?.contains(":") == true)
            settings.set(Settings.GLYPH_DEFAULT_PERMISSION.path, null)
    }
}