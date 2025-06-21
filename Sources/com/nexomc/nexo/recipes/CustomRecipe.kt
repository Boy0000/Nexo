package com.nexomc.nexo.recipes

import com.nexomc.nexo.api.NexoItems
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.inventory.meta.LeatherArmorMeta

class CustomRecipe(
    val name: String,
    val group: String,
    val result: ItemStack,
    val ingredients: List<ItemStack?>,
    ordered: Boolean
) {

    private var isOrdered = ordered

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other is CustomRecipe) return result == other.result && areEqual(ingredients, other.ingredients)
        return false
    }

    override fun hashCode(): Int {
        return result.hashCode() / 2 + ingredients.hashCode() / 2 + (if (isOrdered) 1 else 0)
    }

    private fun areEqual(ingredients1: List<ItemStack?>, ingredients2: List<ItemStack?>): Boolean {
        for (index in ingredients1.indices) {
            val ingredient1 = ingredients1[index]
            when {
                isOrdered -> {
                    val ingredient2 = ingredients2[index]
                    if (ingredient1 == null && ingredient2 == null) continue
                    if (ingredient1 == null || ingredient2 == null) return false
                    if (!ingredient1.isSimilar(ingredient2)) return false
                }
                ingredient1 != null && ingredients2.none(ingredient1::isSimilar) -> return false
            }
        }
        return true
    }

    val isValidDyeRecipe: Boolean
        get() {
            if (!isDyeRecipe) return false
            val items = ingredients.filter { i -> i != null && !i.type.toString().endsWith("_DYE") }
            if (items.size != 1) return false
            val item = items[0] ?: return false
            return !NexoItems.exists(item) || !item.hasItemMeta() || (item.itemMeta !is LeatherArmorMeta) || item.type == Material.LEATHER_HORSE_ARMOR
        }

    private val isDyeRecipe: Boolean
        get() = ingredients.filterNotNull().size == 2 && ingredients.any { item ->
            item?.type.toString().endsWith("_DYE")
        }

    companion object {
        fun fromRecipe(bukkitRecipe: Recipe?): CustomRecipe? {
            when (bukkitRecipe) {
                is ShapedRecipe -> {
                    val ingredients = ArrayList<ItemStack?>(9)
                    for (rowIndex in 0..2) {
                        val row = bukkitRecipe.shape.getOrNull(rowIndex) ?: ""
                        val chars = row.padEnd(3, '_')
                        for (char in chars) ingredients.add(bukkitRecipe.ingredientMap[char])
                    }

                    return CustomRecipe(bukkitRecipe.key.key, bukkitRecipe.group, bukkitRecipe.result, ingredients, true)
                }

                is ShapelessRecipe -> {
                    val ingredients = ArrayList<ItemStack?>(9)
                    ingredients.addAll(bukkitRecipe.ingredientList)
                    return CustomRecipe(bukkitRecipe.key.key, bukkitRecipe.group, bukkitRecipe.result, ingredients, false)
                }

                else -> return null
            }
        }
    }
}
