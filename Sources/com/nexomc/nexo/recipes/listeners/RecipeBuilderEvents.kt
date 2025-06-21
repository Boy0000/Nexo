package com.nexomc.nexo.recipes.listeners

import com.nexomc.nexo.recipes.builders.RecipeBuilder
import com.nexomc.nexo.utils.InventoryUtils.titleFromView
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack

class RecipeBuilderEvents : Listener {
    private val empty = ItemStack(Material.AIR)

    @EventHandler(priority = EventPriority.HIGH)
    @Suppress("DEPRECATION")
    fun InventoryClickEvent.setCursor() {
        val builder = RecipeBuilder.currentBuilder(whoClicked.uniqueId) ?: return
        if (titleFromView(this) != builder.inventoryTitle) return
        if (!builder.validSlot(slot, slotType)) return

        isCancelled = true
        val (currentResult, currentCursor) = (currentItem ?: empty).clone() to cursor.clone()

        if (isLeftClick)  {
            currentItem = currentCursor
            setCursor(currentResult)
        } else if (isRightClick) {
            currentItem = currentCursor.clone().apply { amount = 1 }
            setCursor(currentCursor.subtract(1))
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun InventoryCloseEvent.onInventoryClosed() {
        val recipeBuilder = RecipeBuilder.currentBuilder(player.uniqueId) ?: return
        if (titleFromView(this) != recipeBuilder.inventoryTitle) return

        recipeBuilder.setInventory(inventory)
    }
}
