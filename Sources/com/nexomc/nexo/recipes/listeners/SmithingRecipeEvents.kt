package com.nexomc.nexo.recipes.listeners

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.mechanics.misc.misc.MiscMechanicFactory
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareSmithingEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.SmithingTransformRecipe

class SmithingRecipeEvents : Listener {
    @EventHandler
    fun PrepareSmithingEvent.onSmithingRecipe() {
        val (template, material) = inventory.let { (it.inputTemplate ?: return) to (it.inputMineral ?: return) }
        val input = inventory.inputEquipment ?: return

        if (template.isEmpty || material.isEmpty || input.isEmpty) return
        if (inventory.contents.filterNotNull().any(ItemStack::isEmpty)) return
        if (inventory.contents.filterNotNull().none(NexoItems::exists)) return

        val nexoItemId = NexoItems.idFromItem(input) ?: return
        if (MiscMechanicFactory.instance()?.getMechanic(input)?.isAllowedInVanillaRecipes == true) return

        Bukkit.recipeIterator().forEachRemaining { recipe: Recipe ->
            if (recipe !is SmithingTransformRecipe) return@forEachRemaining
            if (!recipe.template.test(template) || !recipe.addition.test(material)) return@forEachRemaining
            if (nexoItemId == NexoItems.idFromItem(recipe.base.itemStack)) return@forEachRemaining
            result = null
        }
    }
}
