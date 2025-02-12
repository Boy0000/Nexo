package com.nexomc.nexo.utils.inventories

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.fonts.Shift
import com.nexomc.nexo.items.ItemBuilder
import com.nexomc.nexo.recipes.CustomRecipe
import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import com.nexomc.nexo.configs.Settings
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent

class RecipesView {

    fun create(page: Int, filteredRecipes: List<CustomRecipe>): ChestGui {
        val gui = ChestGui(6, Settings.NEXO_RECIPE_SHOWCASE_TITLE.toString())
        val currentRecipe = filteredRecipes[page]

        // Check if last page
        val lastPage = filteredRecipes.size - 1 == page
        val pane = StaticPane(9, 6)
        pane.addItem(GuiItem(currentRecipe.result), 4, 0)

        for (i in currentRecipe.ingredients.indices) {
            val itemStack = currentRecipe.ingredients[i]?.takeUnless { it.type == Material.AIR } ?: continue
            pane.addItem(GuiItem(itemStack), 3 + i % 3, 2 + i / 3)
        }

        // Close RecipeShowcase inventory button
        val exitBuilder = NexoItems.itemFromId("exit_icon") ?: ItemBuilder(Material.BARRIER)
        val exitItem = exitBuilder.displayName(Message.EXIT_MENU.toComponent()).build()
        pane.addItem(GuiItem(exitItem) { event: InventoryClickEvent -> event.whoClicked.closeInventory() }, 4, 5)

        // Previous Page button
        if (page > 0) {
            val builder = NexoItems.itemFromId("arrow_previous_icon") ?: ItemBuilder(Material.ARROW)
            val guiItem = builder.setAmount(page).displayName(Component.text("Open page $page", NamedTextColor.YELLOW)).build()
            pane.addItem(
                GuiItem(guiItem) { e: InventoryClickEvent ->
                    create(page - 1, filteredRecipes).show(e.whoClicked)
                }, 1, 3
            )
        }

        // Next page button
        if (!lastPage) {
            val builder = NexoItems.itemFromId("arrow_next_icon") ?: ItemBuilder(Material.ARROW)
            val guiItem = builder.setAmount(page + 2).displayName(Component.text("Open page ${(page + 2)}", NamedTextColor.YELLOW)).build()
            pane.addItem(
                GuiItem(guiItem) { e ->
                    create(page + 1, filteredRecipes).show(e.whoClicked)
                }, 7, 3
            )
        }

        gui.addPane(pane)
        gui.setOnGlobalClick { event -> event.isCancelled = true }
        return gui
    }
}
