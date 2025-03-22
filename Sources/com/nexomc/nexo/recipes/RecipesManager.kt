package com.nexomc.nexo.recipes

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.recipes.listeners.RecipeBuilderEvents
import com.nexomc.nexo.recipes.listeners.RecipeEventManager
import com.nexomc.nexo.recipes.loaders.BlastingLoader
import com.nexomc.nexo.recipes.loaders.BrewingLoader
import com.nexomc.nexo.recipes.loaders.FurnaceLoader
import com.nexomc.nexo.recipes.loaders.ShapedLoader
import com.nexomc.nexo.recipes.loaders.ShapelessLoader
import com.nexomc.nexo.recipes.loaders.SmokingLoader
import com.nexomc.nexo.recipes.loaders.StonecuttingLoader
import com.nexomc.nexo.utils.AdventureUtils.tagResolver
import com.nexomc.nexo.utils.NexoYaml.Companion.loadConfiguration
import com.nexomc.nexo.utils.childSections
import com.nexomc.nexo.utils.logs.Logs
import java.io.File
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection

object RecipesManager {
    private val recipeFileNames = arrayOf("furnace", "shaped", "shapeless", "blasting", "campfire", "smoking", "stonecutting", "brewing")
    fun load() {
        RecipeEventManager.instance().resetRecipes()
        Bukkit.getPluginManager().registerEvents(RecipeBuilderEvents(), NexoPlugin.instance())
        val recipesFolder = File(NexoPlugin.instance().dataFolder, "recipes").apply(File::mkdirs)
        if (!recipesFolder.exists()) {
            if (Settings.GENERATE_DEFAULT_CONFIGS.toBool())
                NexoPlugin.instance().resourceManager().extractConfigsInFolder("recipes", "yml")
            else runCatching {
                recipeFileNames.forEach { File(recipesFolder, "$it.yml").createNewFile() }
            }.onFailure {
                Logs.logError("Error while creating recipes files: ${it.message}")
            }
        }

        recipesFolder.listFiles()?.forEach(::registerConfigRecipes)
        RecipeEventManager.instance().registerEvents()
    }

    @JvmStatic
    fun reload() {
        RecipeEventManager.instance().resetRecipes()
        val recipesFolder = NexoPlugin.instance().dataFolder.resolve("recipes")
        if (!recipesFolder.exists()) {
            recipesFolder.mkdirs()
            if (Settings.GENERATE_DEFAULT_CONFIGS.toBool())
                NexoPlugin.instance().resourceManager().extractConfigsInFolder("recipes", "yml")
        }

        recipesFolder.listFiles()?.forEach(::registerConfigRecipes)
        RecipeEventManager.instance().registerEvents()
    }

    private fun registerConfigRecipes(configFile: File) {
        loadConfiguration(configFile).childSections().forEach { key, section ->
            registerRecipeByType(configFile, section)
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
                "brewing.yml" -> BrewingLoader(recipeSection).registerRecipe()
            }
        }.onFailure {
            Message.BAD_RECIPE.log(tagResolver("recipe", recipeSection.name))
        }
    }
}
