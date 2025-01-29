package com.nexomc.nexo.recipes.loaders

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.BlastingRecipe

class BlastingLoader(section: ConfigurationSection) : RecipeLoader(section) {
    override fun registerRecipe() {
        val inputSection = section.getConfigurationSection("input") ?: return
        val recipeChoice = recipeChoice(inputSection) ?: return
        val recipe = BlastingRecipe(
            key, result,
            recipeChoice, section.getInt("experience").toFloat(), section.getInt("cookingTime")
        )
        recipe.group = section.getString("group", "")!!
        loadRecipe(recipe)
    }
}
