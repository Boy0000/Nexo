package com.nexomc.nexo.recipes.builders

import com.nexomc.nexo.recipes.RecipeType
import com.nexomc.nexo.utils.deserialize
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory

class BrewingBuilder(player: Player) :
    RecipeBuilder(player, RecipeType.BREWING, "<glyph:brewing_stand_recipe_builder>".deserialize())
{
    override fun createInventory(player: Player?, inventoryTitle: Component): Inventory {
        return Bukkit.createInventory(player, InventoryType.BREWING, inventoryTitle)
    }

    override fun saveRecipe(name: String, permission: String?) {
        val ingredient = inventory.contents.getOrNull(3) ?: return
        val input = inventory.contents.getOrNull(0) ?: return
        val result = inventory.contents.getOrNull(2) ?: return
        val newCraftSection = config.createSection(name)
        setItemStack(newCraftSection.createSection("result"), result)
        setItemStack(newCraftSection.createSection("ingredient"), ingredient)
        setItemStack(newCraftSection.createSection("input"), input)

        if (!permission.isNullOrEmpty()) newCraftSection["permission"] = permission

        saveConfig()
        close()
    }

}