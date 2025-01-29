package com.nexomc.nexo.recipes.loaders

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ShapelessRecipe

class ShapelessLoader(section: ConfigurationSection) : RecipeLoader(section) {
    override fun registerRecipe() {
        val recipe = ShapelessRecipe(key, result)
        recipe.group = section.getString("group", "")!!

        section.getConfigurationSection("ingredients")?.let { it.getKeys(false).map(it::getConfigurationSection) }
            ?.forEach { itemSection ->
                val ingredient = itemSection?.let(::recipeChoice) ?: return@forEach
                val amount = itemSection.getInt("amount")
                repeat(amount) { recipe.addIngredient(ingredient) }
            }
        addToWhitelist(recipe)
        loadRecipe(recipe)
    }
}
