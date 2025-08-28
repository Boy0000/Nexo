package com.nexomc.nexo.recipes.loaders

import com.nexomc.nexo.utils.getEnum
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.BlastingRecipe
import org.bukkit.inventory.recipe.CookingBookCategory

class BlastingLoader(section: ConfigurationSection) : RecipeLoader(section) {
    override fun registerRecipe() {
        val inputSection = section.getConfigurationSection("input") ?: return
        val recipeChoice = recipeChoice(inputSection) ?: return
        val exp = section.getInt("experience").toFloat()
        val cookingTime = section.getInt("cookingTime")
        val recipe = BlastingRecipe(key, result, recipeChoice, exp, cookingTime)
        recipe.group = section.getString("group", "")!!
        recipe.category = section.getEnum("category", CookingBookCategory::class.java) ?: recipe.category
        loadRecipe(recipe)
    }
}
