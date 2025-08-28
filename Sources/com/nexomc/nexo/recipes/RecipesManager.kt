package com.nexomc.nexo.recipes

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.converter.NexoConverter
import com.nexomc.nexo.recipes.listeners.RecipeBuilderEvents
import com.nexomc.nexo.recipes.listeners.RecipeEventManager
import com.nexomc.nexo.recipes.loaders.BlastingLoader
import com.nexomc.nexo.recipes.loaders.BrewingLoader
import com.nexomc.nexo.recipes.loaders.CampfireLoader
import com.nexomc.nexo.recipes.loaders.FurnaceLoader
import com.nexomc.nexo.recipes.loaders.ShapedLoader
import com.nexomc.nexo.recipes.loaders.ShapelessLoader
import com.nexomc.nexo.recipes.loaders.SmokingLoader
import com.nexomc.nexo.recipes.loaders.StonecuttingLoader
import com.nexomc.nexo.utils.AdventureUtils.tagResolver
import com.nexomc.nexo.utils.NexoYaml
import com.nexomc.nexo.utils.childSections
import com.nexomc.nexo.utils.listYamlFiles
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import java.io.File

object RecipesManager {
    val recipesFolder = File(NexoPlugin.instance().dataFolder, "recipes").apply(File::mkdirs)

    fun load() {
        NexoConverter.processRecipes()
        RecipeEventManager.instance().resetRecipes()
        Bukkit.getPluginManager().registerEvents(RecipeBuilderEvents(), NexoPlugin.instance())

        RecipeType.entries.forEach(::registerConfigRecipes)
        RecipeEventManager.instance().registerEvents()
    }

    @JvmStatic
    fun reload() {
        NexoConverter.processRecipes()
        RecipeEventManager.instance().resetRecipes()
        val recipesFolder = NexoPlugin.instance().dataFolder.resolve("recipes")
        if (!recipesFolder.exists()) {
            recipesFolder.mkdirs()
            if (Settings.GENERATE_DEFAULT_CONFIGS.toBool())
                NexoPlugin.instance().resourceManager().extractConfigsInFolder("recipes", "yml")
        }

        RecipeType.entries.forEach(::registerConfigRecipes)
        RecipeEventManager.instance().registerEvents()
    }

    private fun registerConfigRecipes(recipeType: RecipeType) {
        recipesFolder.resolve(recipeType.id).listYamlFiles(true).forEach {
            NexoYaml.loadConfiguration(it).childSections().forEach { key, section ->
                registerRecipeByType(recipeType, section)
            }
        }
    }

    private fun registerRecipeByType(recipeType: RecipeType, recipeSection: ConfigurationSection) {
        runCatching {
            when (recipeType) {
                RecipeType.SHAPED -> ShapedLoader(recipeSection).registerRecipe()
                RecipeType.SHAPELESS -> ShapelessLoader(recipeSection).registerRecipe()
                RecipeType.FURNACE -> FurnaceLoader(recipeSection).registerRecipe()
                RecipeType.BLASTING -> BlastingLoader(recipeSection).registerRecipe()
                RecipeType.SMOKER -> SmokingLoader(recipeSection).registerRecipe()
                RecipeType.STONECUTTING -> StonecuttingLoader(recipeSection).registerRecipe()
                RecipeType.BREWING -> BrewingLoader(recipeSection).registerRecipe()
                RecipeType.CAMPFIRE -> CampfireLoader(recipeSection).registerRecipe()
            }
        }.onFailure {
            Message.BAD_RECIPE.log(tagResolver("recipe", recipeSection.name))
        }
    }
}
