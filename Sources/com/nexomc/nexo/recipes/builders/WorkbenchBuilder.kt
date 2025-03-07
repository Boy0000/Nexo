package com.nexomc.nexo.recipes.builders

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory

abstract class WorkbenchBuilder(player: Player, builderName: String) : RecipeBuilder(player, builderName) {
    override fun createInventory(player: Player?, inventoryTitle: Component): Inventory {
        return Bukkit.createInventory(player, InventoryType.WORKBENCH, inventoryTitle)
    }
}