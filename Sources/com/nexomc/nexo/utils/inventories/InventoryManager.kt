package com.nexomc.nexo.utils.inventories

import com.nexomc.nexo.recipes.CustomRecipe
import dev.triumphteam.gui.guis.PaginatedGui
import org.bukkit.entity.Player
import java.util.*

class InventoryManager {
    private val itemsViews = mutableMapOf<UUID, PaginatedGui>()

    init {
        regen()
    }

    fun regen() {
        itemsViews.clear()
    }

    fun itemsView(player: Player) = itemsViews.computeIfAbsent(player.uniqueId) { ItemsView().create() }

    fun recipesShowcase(page: Int, filteredRecipes: List<CustomRecipe>) = RecipesView().create(page, filteredRecipes)
}
