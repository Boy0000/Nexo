package com.nexomc.nexo.recipes.builders

import com.nexomc.nexo.recipes.RecipeType
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory

class CampfireBuilder(player: Player) : CookingBuilder(player, RecipeType.CAMPFIRE) {
    override fun createInventory(player: Player?, inventoryTitle: Component): Inventory {
        return Bukkit.createInventory(player, InventoryType.SMOKER, inventoryTitle)
    }
}
