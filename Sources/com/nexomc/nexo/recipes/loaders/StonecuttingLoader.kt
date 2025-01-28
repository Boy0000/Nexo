package com.nexomc.nexo.recipes.loaders

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.StonecuttingRecipe

class StonecuttingLoader(section: ConfigurationSection) : RecipeLoader(section) {
    override fun registerRecipe() {
        val inputSection = section.getConfigurationSection("input") ?: return
        val recipeChoice = recipeChoice(inputSection) ?: return
        val recipe = StonecuttingRecipe(namespacedKey, result, recipeChoice)
        recipe.group = section.getString("group", "")!!
        loadRecipe(recipe)
    }
}
