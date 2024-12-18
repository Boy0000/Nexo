package com.nexomc.nexo.recipes.builders

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory

abstract class WorkbenchBuilder protected constructor(player: Player, builderName: String) :
    RecipeBuilder(player, builderName) {
    override fun createInventory(player: Player?, inventoryTitle: String): Inventory {
        return Bukkit.createInventory(player, InventoryType.WORKBENCH, inventoryTitle)
    }
}