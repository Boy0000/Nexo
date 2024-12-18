package com.nexomc.nexo.recipes.listeners

import com.nexomc.nexo.recipes.builders.RecipeBuilder
import com.nexomc.nexo.utils.InventoryUtils.getTitleFromView
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack

class RecipeBuilderEvents : Listener {
    private val empty = ItemStack(Material.AIR)

    @EventHandler(priority = EventPriority.HIGH)
    @Suppress("DEPRECATION")
    fun InventoryClickEvent.setCursor() {
        val recipeBuilderTitle = RecipeBuilder.get(whoClicked.uniqueId)?.inventoryTitle
        if (getTitleFromView(this) != recipeBuilderTitle || slotType != InventoryType.SlotType.RESULT) return

        isCancelled = true
        val (currentResult, currentCursor) = (currentItem ?: empty).clone() to cursor.clone()
        currentItem = currentCursor
        setCursor(currentResult)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun InventoryCloseEvent.onInventoryClosed() {
        val recipeBuilder = RecipeBuilder.get(player.uniqueId) ?: return
        if (getTitleFromView(this) != recipeBuilder.inventoryTitle) return

        recipeBuilder.setInventory(inventory)
    }
}
