package com.nexomc.nexo.recipes.loaders

import io.papermc.paper.potion.PotionMix
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapelessRecipe

class BrewingLoader(section: ConfigurationSection) : RecipeLoader(section) {

    override fun registerRecipe() {
        val input = section.getConfigurationSection("input")?.let(::recipeChoice)?.let { PotionMix.createPredicateChoice { i -> it.test(i) } }!!
        val ingredient = section.getConfigurationSection("ingredient")?.let(::recipeChoice)?.let { PotionMix.createPredicateChoice { i -> it.test(i) } }!!

        Bukkit.getServer().potionBrewer.removePotionMix(key)
        Bukkit.getServer().potionBrewer.addPotionMix(PotionMix(key, result, input, ingredient))
    }
}
