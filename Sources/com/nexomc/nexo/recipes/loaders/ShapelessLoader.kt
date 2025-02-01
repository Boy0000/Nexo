package com.nexomc.nexo.recipes.loaders

import com.nexomc.nexo.utils.childSections
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ShapelessRecipe

class ShapelessLoader(section: ConfigurationSection) : RecipeLoader(section) {
    override fun registerRecipe() {
        val recipe = ShapelessRecipe(key, result)
        recipe.group = section.getString("group", "")!!

        section.getConfigurationSection("ingredients")?.childSections()?.forEach { (_, itemSection) ->
            val ingredient = recipeChoice(itemSection) ?: return@forEach
            repeat(itemSection.getInt("amount")) { recipe.addIngredient(ingredient) }
        }

        addToWhitelist(recipe)
        loadRecipe(recipe)
    }
}
