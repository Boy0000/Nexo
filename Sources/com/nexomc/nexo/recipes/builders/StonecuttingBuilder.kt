package com.nexomc.nexo.recipes.builders

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory

class StonecuttingBuilder(player: Player) :
    RecipeBuilder(player, "stonecutting") {
    override fun createInventory(player: Player?, inventoryTitle: String): Inventory {
        return Bukkit.createInventory(player, InventoryType.WORKBENCH, "<glyph:recipe_stonecutter>")
    }

    override fun saveRecipe(name: String, permission: String?) {
        val input = inventory.getItem(0)
        var recipeCount = 0
        for (i in 1 until inventory.size) {
            val result = inventory.getItem(i) ?: continue
            val newCraftSection = getConfig()!!.createSection(name + "_" + recipeCount)
            setItemStack(newCraftSection.createSection("result"), result)
            setItemStack(newCraftSection.createSection("input"), input!!)

            if (!permission.isNullOrEmpty()) newCraftSection["permission"] = permission

            saveConfig()
            recipeCount++
        }
        close()
    }
}
