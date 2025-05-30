package com.nexomc.nexo.utils.inventories

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.items.ItemBuilder
import com.nexomc.nexo.recipes.CustomRecipe
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material

class RecipesView {

    fun create(page: Int, filteredRecipes: List<CustomRecipe>): Gui {
        val gui = Gui.gui().rows(6).title(Settings.NEXO_RECIPE_SHOWCASE_TITLE.toComponent()).create()
        val currentRecipe = filteredRecipes[page]

        // Check if last page
        val lastPage = filteredRecipes.size - 1 == page
        gui.setItem(4, 0, GuiItem(currentRecipe.result))

        for (i in currentRecipe.ingredients.indices) {
            val itemStack = currentRecipe.ingredients[i]?.takeUnless { it.isEmpty } ?: continue
            gui.setItem(2 + i / 3, 3 + i % 3, GuiItem(itemStack))
        }

        // Close RecipeShowcase inventory button
        val exitBuilder = NexoItems.itemFromId("exit_icon") ?: ItemBuilder(Material.BARRIER)
        val exitItem = exitBuilder.displayName(Message.EXIT_MENU.toComponent()).build()
        gui.setItem(5, 4, GuiItem(exitItem) { it.whoClicked.closeInventory() })

        // Previous Page button
        if (page > 0) {
            val builder = NexoItems.itemFromId("arrow_previous_icon") ?: ItemBuilder(Material.ARROW)
            val guiItem = builder.setAmount(page).displayName(Component.text("Open page $page", NamedTextColor.YELLOW)).build()
            gui.setItem(3, 1, GuiItem(guiItem) { create(page - 1, filteredRecipes).open(it.whoClicked) })
        }

        // Next page button
        if (!lastPage) {
            val builder = NexoItems.itemFromId("arrow_next_icon") ?: ItemBuilder(Material.ARROW)
            val guiItem = builder.setAmount(page + 2).displayName(Component.text("Open page ${(page + 2)}", NamedTextColor.YELLOW)).build()
            gui.setItem(3, 7, GuiItem(guiItem) { create(page + 1, filteredRecipes).open(it.whoClicked) })
        }

        gui.setDefaultClickAction { it.isCancelled = true }
        return gui
    }
}
