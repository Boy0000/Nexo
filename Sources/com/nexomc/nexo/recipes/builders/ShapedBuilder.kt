package com.nexomc.nexo.recipes.builders

import com.nexomc.nexo.recipes.RecipeType
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class ShapedBuilder(player: Player) : WorkbenchBuilder(player, RecipeType.SHAPED) {
    override fun saveRecipe(name: String, permission: String?) {
        val letterByItem = mutableMapOf<ItemStack?, Char>()
        var letter = 'A'
        val shapes = arrayOfNulls<String>(3)
        var shape = StringBuilder()

        for (i in 1 until inventory.size) {
            val item = inventory.getItem(i)
            when {
                item == null -> shape.append("_")
                letterByItem.containsKey(item) -> shape.append(letterByItem[item])
                else -> {
                    shape.append(letter)
                    letterByItem[inventory.getItem(i)] = letter
                    letter++
                }
            }

            if (shape.length == 3) {
                shapes[(i + 1) / 3 - 1] = shape.toString()
                shape = StringBuilder()
            }
        }

        val newCraftSection: ConfigurationSection?
        val resultSection: ConfigurationSection?
        val ingredients: ConfigurationSection?
        if (config.isConfigurationSection(name)) {
            newCraftSection = config.getConfigurationSection(name)
            resultSection = newCraftSection!!.getConfigurationSection("result")
            ingredients = newCraftSection.getConfigurationSection("ingredients")
        } else {
            newCraftSection = config.createSection(name)
            resultSection = newCraftSection.createSection("result")
            ingredients = newCraftSection.createSection("ingredients")
        }
        newCraftSection["shape"] = shapes
        setItemStack(resultSection!!, inventory.getItem(0)!!)
        for ((key, value) in letterByItem) {
            val ingredientSection = ingredients!!.createSection(value.toString())
            setItemStack(ingredientSection, key!!)
        }
        if (!permission.isNullOrEmpty()) newCraftSection["permission"] = permission
        saveConfig()
        close()
    }
}
