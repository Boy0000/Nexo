package com.nexomc.nexo.recipes.loaders

import io.papermc.paper.potion.PotionMix
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection

class BrewingLoader(section: ConfigurationSection) : RecipeLoader(section) {

    override fun registerRecipe() {
        val input = section.getConfigurationSection("input")?.let(::recipeChoice)?.let { PotionMix.createPredicateChoice { i -> it.test(i) } }!!
        val ingredient = section.getConfigurationSection("ingredient")?.let(::recipeChoice)?.let { PotionMix.createPredicateChoice { i -> it.test(i) } }!!

        Bukkit.getServer().potionBrewer.removePotionMix(key)
        Bukkit.getServer().potionBrewer.addPotionMix(PotionMix(key, result, input, ingredient))
    }
}
