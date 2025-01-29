package com.nexomc.nexo.recipes.builders

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

class ShapelessBuilder(player: Player) : WorkbenchBuilder(player, "shapeless") {
    override fun saveRecipe(name: String, permission: String?) {
        val items = mutableMapOf<ItemStack?, Int>()
        val content = inventory.contents
        for (i in 1 until content.size) if (content[i] != null)
            items[content[i]] = items.getOrDefault(content[i], 0) + 1

        val newCraftSection = getConfig()!!.createSection(name)
        setItemStack(newCraftSection.createSection("result"), content[0]!!)
        val ingredients = newCraftSection.createSection("ingredients")

        var i = 0

        for ((key, value) in items) {
            val ingredientSection = ingredients.createSection((64 + ++i).toChar().toString())
            ingredientSection["amount"] = value
            setItemStack(ingredientSection, key!!)
        }

        if (!permission.isNullOrEmpty()) newCraftSection["permission"] = permission
        saveConfig()
        close()
    }
}
