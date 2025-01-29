package com.nexomc.nexo.recipes.loaders

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ShapedRecipe
import java.util.*

class ShapedLoader(section: ConfigurationSection) : RecipeLoader(section) {
    override fun registerRecipe() {
        val recipe = ShapedRecipe(key, result)
        recipe.group = section.getString("group", "")!!

        val shape = section.getStringList("shape")
        recipe.shape(*shape.toTypedArray<String>())

        val ingredientsSection = section.getConfigurationSection("ingredients")
        for (ingredientLetter in Objects.requireNonNull<ConfigurationSection?>(ingredientsSection).getKeys(false)) {
            val itemSection = ingredientsSection!!.getConfigurationSection(ingredientLetter) ?: continue
            val recipeChoice = recipeChoice(itemSection) ?: continue
            recipe.setIngredient(ingredientLetter[0], recipeChoice)
        }
        addToWhitelist(recipe)
        loadRecipe(recipe)
    }
}
