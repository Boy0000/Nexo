package com.nexomc.nexo.recipes.builders

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory

class BlastingBuilder(player: Player) : CookingBuilder(player, "blasting") {
    override fun createInventory(player: Player?, inventoryTitle: String): Inventory {
        return Bukkit.createInventory(player, InventoryType.BLAST_FURNACE, inventoryTitle)
    }
}
