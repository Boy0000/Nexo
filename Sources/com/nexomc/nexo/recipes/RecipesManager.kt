package com.nexomc.nexo.recipes

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.recipes.listeners.RecipeBuilderEvents
import com.nexomc.nexo.recipes.listeners.RecipeEventManager
import com.nexomc.nexo.recipes.loaders.*
import com.nexomc.nexo.utils.AdventureUtils.tagResolver
import com.nexomc.nexo.utils.NexoYaml.Companion.loadConfiguration
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.mapNotNullFast
import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

object RecipesManager {
    fun load(plugin: JavaPlugin) {
        if (Settings.RESET_RECIPES.toBool()) {
            val recipeIterator = Bukkit.recipeIterator()
            while (recipeIterator.hasNext()) (recipeIterator.next() as? Keyed)?.key?.takeIf { it.namespace == "nexo" }?.let(Bukkit::removeRecipe)
        }

        Bukkit.getPluginManager().registerEvents(RecipeBuilderEvents(), plugin)
        val recipesFolder = File(NexoPlugin.instance().dataFolder, "recipes").apply(File::mkdirs)
        if (!recipesFolder.exists()) {
            if (Settings.GENERATE_DEFAULT_CONFIGS.toBool()) NexoPlugin.instance().resourceManager()
                .extractConfigsInFolder("recipes", "yml")
            else runCatching {
                File(recipesFolder, "furnace.yml").createNewFile()
                File(recipesFolder, "shaped.yml").createNewFile()
                File(recipesFolder, "shapeless.yml").createNewFile()
                File(recipesFolder, "blasting.yml").createNewFile()
                File(recipesFolder, "campfire.yml").createNewFile()
                File(recipesFolder, "smoking.yml").createNewFile()
                File(recipesFolder, "stonecutting.yml").createNewFile()
            }.onFailure {
                Logs.logError("Error while creating recipes files: ${it.message}")
            }
        }
        registerAllConfigRecipesFromFolder(recipesFolder)
        RecipeEventManager.instance().registerEvents()
    }

    @JvmStatic
    fun reload() {
        if (Settings.RESET_RECIPES.toBool()) {
            val recipeIterator = Bukkit.recipeIterator()
            while (recipeIterator.hasNext()) (recipeIterator.next() as? Keyed)?.key?.takeIf { it.namespace == "nexo" }?.let(Bukkit::removeRecipe)
        }

        RecipeEventManager.instance().resetRecipes()
        val recipesFolder = NexoPlugin.instance().dataFolder.resolve("recipes")
        if (!recipesFolder.exists()) {
            recipesFolder.mkdirs()
            if (Settings.GENERATE_DEFAULT_CONFIGS.toBool())
                NexoPlugin.instance().resourceManager().extractConfigsInFolder("recipes", "yml")
        }
        registerAllConfigRecipesFromFolder(recipesFolder)
        RecipeEventManager.instance().registerEvents()
    }

    private fun registerAllConfigRecipesFromFolder(recipesFolder: File) {
        recipesFolder.listFiles()?.forEach(::registerConfigRecipes)
    }

    private fun registerConfigRecipes(configFile: File) {
        loadConfiguration(configFile).let { it.getKeys(false).mapNotNullFast(it::getConfigurationSection) }.forEach {
            registerRecipeByType(configFile, it)
        }
    }

    private fun registerRecipeByType(configFile: File, recipeSection: ConfigurationSection) {
        runCatching {
            when (configFile.name) {
                "shaped.yml" -> ShapedLoader(recipeSection).registerRecipe()
                "shapeless.yml" -> ShapelessLoader(recipeSection).registerRecipe()
                "furnace.yml" -> FurnaceLoader(recipeSection).registerRecipe()
                "blasting.yml" -> BlastingLoader(recipeSection).registerRecipe()
                "smoking.yml" -> SmokingLoader(recipeSection).registerRecipe()
                "stonecutting.yml" -> StonecuttingLoader(recipeSection).registerRecipe()
            }
        }.onFailure {
            Message.BAD_RECIPE.log(tagResolver("recipe", recipeSection.name))
        }
    }
}
