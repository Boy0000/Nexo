package com.nexomc.nexo.recipes.builders

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory

class CampfireBuilder(player: Player) : CookingBuilder(player, "campfire") {
    override fun createInventory(player: Player?, inventoryTitle: String): Inventory {
        return Bukkit.createInventory(player, InventoryType.SMOKER, inventoryTitle)
    }
}
