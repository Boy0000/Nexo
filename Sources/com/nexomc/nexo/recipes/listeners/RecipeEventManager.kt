package com.nexomc.nexo.recipes.listeners

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.mechanics.misc.misc.MiscMechanicFactory
import com.nexomc.nexo.recipes.CustomRecipe
import com.nexomc.nexo.utils.InventoryUtils.playerFromView
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.command.CommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.MerchantInventory
import kotlin.collections.set

class RecipeEventManager(
    private val permissionsPerRecipe: MutableMap<CustomRecipe, String?> = Object2ObjectOpenHashMap<CustomRecipe, String>(),
    private val whitelistedCraftRecipes: MutableSet<CustomRecipe> = ObjectOpenHashSet(),
    private val whitelistedCraftRecipesOrdered: MutableList<CustomRecipe> = ObjectArrayList()
) : Listener {

    fun registerEvents() {
        Bukkit.getPluginManager().registerEvents(instance!!, NexoPlugin.instance())
        Bukkit.getPluginManager().registerEvents(SmithingRecipeEvents(), NexoPlugin.instance())
    }

    @EventHandler(ignoreCancelled = true)
    fun InventoryClickEvent.onTrade() {
        val inventory = inventory as? MerchantInventory ?: return
        val selectedRecipe = inventory.selectedRecipe?.takeIf { slot == 2 } ?: return

        val first = NexoItems.idFromItem(inventory.getItem(0))
        val second = NexoItems.idFromItem(inventory.getItem(1))
        val ingredients = selectedRecipe.ingredients
        val firstIngredient = NexoItems.idFromItem(ingredients[0])
        val secondIngredient = if (ingredients.size < 2) null else NexoItems.idFromItem(ingredients[1])
        if (first != firstIngredient || second != secondIngredient) isCancelled = true
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    fun PrepareItemCraftEvent.onCrafted() {
        val (customRecipe, player) = CustomRecipe.fromRecipe(recipe) to (playerFromView(this) ?: return)
        if (!hasPermission(player, customRecipe)) inventory.result = null
        if (inventory.result == null || recipe == null || customRecipe == null || customRecipe.isValidDyeRecipe || MiscMechanicFactory.instance() == null) return
        if (inventory.matrix.any { MiscMechanicFactory.instance()?.getMechanic(it)?.isAllowedInVanillaRecipes == false }) {
            inventory.result = null
            return
        }
        if (inventory.matrix.none(NexoItems::exists) || whitelistedCraftRecipes.any(customRecipe::equals)) return

        inventory.result = customRecipe.result
    }

    @EventHandler
    fun PlayerJoinEvent.onJoin() {
        if (!Settings.ADD_RECIPES_TO_BOOK.toBool()) return
        player.discoverRecipes(permittedRecipes(player).mapNotNull { NamespacedKey.fromString(it.name, NexoPlugin.instance()) }.toSet())
    }

    fun resetRecipes() {
        permissionsPerRecipe.clear()
        whitelistedCraftRecipes.clear()
        whitelistedCraftRecipesOrdered.clear()
    }

    fun addPermissionRecipe(recipe: CustomRecipe, permission: String?) {
        permissionsPerRecipe[recipe] = permission
    }

    fun whitelistRecipe(recipe: CustomRecipe) {
        whitelistedCraftRecipes += recipe
        whitelistedCraftRecipesOrdered += recipe
    }

    fun permittedRecipes(sender: CommandSender): List<CustomRecipe> {
        return whitelistedCraftRecipesOrdered.filter { customRecipe ->
            customRecipe !in permissionsPerRecipe || hasPermission(sender, customRecipe)
        }
    }

    fun permittedRecipeNames(sender: CommandSender) = permittedRecipes(sender).map { it.name }.toTypedArray()


    fun hasPermission(sender: CommandSender, recipe: CustomRecipe?) =
        recipe !in permissionsPerRecipe || sender.hasPermission(permissionsPerRecipe[recipe]!!)

    companion object {
        private var instance: RecipeEventManager? = null
        @JvmStatic
        fun instance(): RecipeEventManager {
            if (instance == null) instance = RecipeEventManager()
            return instance!!
        }
    }
}
