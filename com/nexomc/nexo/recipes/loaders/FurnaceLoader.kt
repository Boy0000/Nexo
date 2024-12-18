package com.nexomc.nexo.recipes.loaders

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.FurnaceRecipe

class FurnaceLoader(section: ConfigurationSection) : RecipeLoader(section) {
    override fun registerRecipe() {
        val inputSection = section.getConfigurationSection("input") ?: return
        val recipeChoice = recipeChoice(inputSection) ?: return
        val recipe = FurnaceRecipe(
            namespacedKey, result,
            recipeChoice, section.getInt("experience").toFloat(), section.getInt("cookingTime")
        )
        loadRecipe(recipe)
    }
}
