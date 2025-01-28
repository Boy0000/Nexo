package com.nexomc.nexo.recipes

import com.nexomc.nexo.api.NexoItems
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.inventory.meta.LeatherArmorMeta
import java.util.*

class CustomRecipe {

    val name: String
    val group: String
    val result: ItemStack
    val ingredients: List<ItemStack?>
    var isOrdered = false

    constructor(name: String, result: ItemStack, ingredients: List<ItemStack?>) {
        this.name = name
        this.group = ""
        this.result = result
        this.ingredients = ingredients
    }

    constructor(name: String, group: String, result: ItemStack, ingredients: List<ItemStack?>) {
        this.name = name
        this.group = group
        this.result = result
        this.ingredients = ingredients
    }

    constructor(name: String, result: ItemStack, ingredients: List<ItemStack?>, ordered: Boolean) {
        this.name = name
        this.group = ""
        this.result = result
        this.ingredients = ingredients
        this.isOrdered = ordered
    }

    constructor(name: String, group: String, result: ItemStack, ingredients: List<ItemStack?>, ordered: Boolean) {
        this.name = name
        this.group = group
        this.result = result
        this.ingredients = ingredients
        this.isOrdered = ordered
    }

    override fun equals(`object`: Any?): Boolean {
        if (`object` == null) return false
        if (`object` === this) return true
        if (`object` is CustomRecipe) return result == `object`.result && areEqual(ingredients, `object`.ingredients)
        return false
    }

    override fun hashCode(): Int {
        return result.hashCode() / 2 + ingredients.hashCode() / 2 + (if (isOrdered) 1 else 0)
    }

    private fun areEqual(ingredients1: List<ItemStack?>, ingredients2: List<ItemStack?>): Boolean {
        for (index in ingredients1.indices) {
            val ingredient1 = ingredients1[index]
            if (isOrdered) {
                val ingredient2 = ingredients2[index]
                if (ingredient1 == null && ingredient2 == null) continue
                if (ingredient1 == null || ingredient2 == null) return false
                if (!ingredient1.isSimilar(ingredient2)) return false
            } else if (ingredient1 != null && ingredients2.stream()
                    .noneMatch { stack: ItemStack? -> ingredient1.isSimilar(stack) }
            ) return false
        }
        return true
    }

    val isValidDyeRecipe: Boolean
        /**
         * Checks if the recipe is a dye recipe.
         * This does not ensure the second ingredient is dyeable,
         * only that the ingredient is not CustomArmor and therefore dyeable
         */
        get() {
            if (!isDyeRecipe) return false
            val items = ingredients.stream()
                .filter { i: ItemStack? -> i != null && !i.type.toString().endsWith("_DYE") }
                .toList()
            if (items.size != 1) return false
            val item = items[0] ?: return false
            return !NexoItems.exists(item) || !item.hasItemMeta() || (item.itemMeta !is LeatherArmorMeta) || item.type == Material.LEATHER_HORSE_ARMOR
        }

    private val isDyeRecipe: Boolean
        get() = ingredients.stream().filter { obj: ItemStack? -> Objects.nonNull(obj) }
            .toList().size == 2 && ingredients.stream().anyMatch { item: ItemStack? ->
            item != null && item.type.toString().endsWith("_DYE")
        }

    companion object {
        fun fromRecipe(bukkitRecipe: Recipe?): CustomRecipe? {
            when (bukkitRecipe) {
                is ShapedRecipe -> {
                    val ingredients = ArrayList<ItemStack?>(9)
                    for (row in bukkitRecipe.shape) {
                        val chars = row.toCharArray()
                        for (charIndex in 0..2) {
                            if (charIndex >= chars.size) {
                                ingredients.add(null)
                                continue
                            }
                            ingredients.add(bukkitRecipe.ingredientMap[chars[charIndex]])
                        }
                    }
                    return CustomRecipe(bukkitRecipe.key.key, bukkitRecipe.group, bukkitRecipe.getResult(), ingredients, true)
                }

                is ShapelessRecipe -> {
                    val ingredients = ArrayList<ItemStack?>(9)
                    ingredients.addAll(bukkitRecipe.ingredientList)
                    return CustomRecipe(bukkitRecipe.key.key, bukkitRecipe.group, bukkitRecipe.getResult(), ingredients, false)
                }

                else -> return null
            }
        }
    }
}
