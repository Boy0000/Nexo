package com.nexomc.nexo.recipes.builders

import com.nexomc.nexo.recipes.RecipeType
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory

class StonecuttingBuilder(player: Player) : RecipeBuilder(player, RecipeType.STONECUTTING) {
    override fun createInventory(player: Player?, inventoryTitle: Component): Inventory {
        return Bukkit.createInventory(player, InventoryType.WORKBENCH, inventoryTitle)
    }

    override fun saveRecipe(name: String, permission: String?) {
        val input = inventory.getItem(0)
        var recipeCount = 0
        for (i in 1 until inventory.size) {
            val result = inventory.getItem(i) ?: continue
            val newCraftSection = config.createSection(name + "_" + recipeCount)
            setItemStack(newCraftSection.createSection("result"), result)
            setItemStack(newCraftSection.createSection("input"), input!!)

            if (!permission.isNullOrEmpty()) newCraftSection["permission"] = permission

            saveConfig()
            recipeCount++
        }
        close()
    }
}
