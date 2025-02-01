package com.nexomc.nexo.recipes.loaders

import com.nexomc.nexo.utils.childSections
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ShapedRecipe

class ShapedLoader(section: ConfigurationSection) : RecipeLoader(section) {
    override fun registerRecipe() {
        val recipe = ShapedRecipe(key, result)
        recipe.group = section.getString("group", "")!!
        recipe.shape(*section.getStringList("shape").toTypedArray())

        section.getConfigurationSection("ingredients")?.childSections()?.forEach { (letter, itemSection) ->
            recipe.setIngredient(letter.toCharArray().first(), recipeChoice(itemSection) ?: return@forEach)
        }

        addToWhitelist(recipe)
        loadRecipe(recipe)
    }
}
