package com.nexomc.nexo.recipes.loaders

import com.nexomc.nexo.utils.childSections
import com.nexomc.nexo.utils.getEnum
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.recipe.CraftingBookCategory

class ShapedLoader(section: ConfigurationSection) : RecipeLoader(section) {
    override fun registerRecipe() {
        val recipe = ShapedRecipe(key, result)
        recipe.group = section.getString("group", "")!!
        recipe.shape(*section.getStringList("shape").toTypedArray())
        recipe.category = section.getEnum("category", CraftingBookCategory::class.java) ?: recipe.category

        section.getConfigurationSection("ingredients")?.childSections()?.forEach { (letter, itemSection) ->
            recipe.setIngredient(letter.toCharArray().first(), recipeChoice(itemSection) ?: return@forEach)
        }

        addToWhitelist(recipe)
        loadRecipe(recipe)
    }
}
