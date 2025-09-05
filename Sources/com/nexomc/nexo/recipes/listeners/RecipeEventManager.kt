package com.nexomc.nexo.recipes.listeners

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.recipes.CustomRecipe
import com.nexomc.nexo.utils.InventoryUtils.playerFromView
import com.nexomc.nexo.utils.ItemUtils
import com.nexomc.nexo.utils.safeCast
import io.papermc.paper.command.brigadier.argument.ArgumentTypes.player
import io.papermc.paper.event.player.PlayerStonecutterRecipeSelectEvent
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.NamespacedKey
import org.bukkit.block.Furnace
import org.bukkit.command.CommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.FurnaceBurnEvent
import org.bukkit.event.inventory.FurnaceSmeltEvent
import org.bukkit.event.inventory.FurnaceStartSmeltEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.MerchantInventory

class RecipeEventManager(
    private val permissionsPerRecipe: Object2ObjectOpenHashMap<CustomRecipe, String> = Object2ObjectOpenHashMap<CustomRecipe, String>(),
    private val whitelistedCraftRecipes: ObjectOpenHashSet<CustomRecipe> = ObjectOpenHashSet(),
    private val whitelistedCraftRecipesOrdered: ObjectArrayList<CustomRecipe> = ObjectArrayList()
) : Listener {

    fun registerEvents() {
        Bukkit.getPluginManager().registerEvents(instance!!, NexoPlugin.instance())
        Bukkit.getPluginManager().registerEvents(SmithingRecipeEvents(), NexoPlugin.instance())
    }

    fun d() {
        val words = listOf("Four", "Five", "Nine")
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
        val recipeKey = recipe.safeCast<Keyed>()?.key ?: return
        val isValidDyeRecipe = recipeKey.asString() == "minecraft:armor_dye"
        val isVanillaRecipe = recipeKey.namespace == "minecraft" && !isValidDyeRecipe
        val customRecipe = CustomRecipe.fromRecipe(recipe)
        val player = playerFromView(this) ?: return
        if (!hasPermission(player, customRecipe)) inventory.result = null
        if (inventory.result == null || recipe == null || inventory.matrix.none(NexoItems::exists)) return

        if (isVanillaRecipe && inventory.matrix.any { !ItemUtils.isAllowedInVanillaRecipes(it) }) inventory.result = null
        if (customRecipe == null || isValidDyeRecipe || whitelistedCraftRecipes.none(customRecipe::equals)) return

        inventory.result = customRecipe.result
    }

    @EventHandler
    fun FurnaceBurnEvent.onBurn() {
        val furnace = block.state.safeCast<Furnace>() ?: return
        val recipe = furnace.recipesUsed.firstNotNullOfOrNull { CustomRecipe.fromRecipe(it.key) } ?: return
        val player = furnace.inventory.viewers.firstOrNull()

        when {
            recipe.safeCast<Keyed>()?.key?.namespace == "minecraft" -> return
            player == null && recipe in permissionsPerRecipe && Settings.RECIPES_REQUIRE_PLAYER_IF_PERMISSION.toBool() -> isCancelled = true
            player != null && !hasPermission(player, recipe) -> isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    fun PlayerStonecutterRecipeSelectEvent.onGrindstone() {
        if (!hasPermission(player, CustomRecipe.fromRecipe(stonecuttingRecipe))) isCancelled = true
        if (!ItemUtils.isAllowedInVanillaRecipes(stonecutterInventory.inputItem)) isCancelled = true
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

        if (Settings.RESET_RECIPES.toBool()) {
            Bukkit.recipeIterator().forEachRemaining recipes@{
                Bukkit.removeRecipe((it as? Keyed)?.key?.takeIf { r -> r.namespace == "nexo" } ?: return@recipes, false)
            }
            Bukkit.getServer().potionBrewer.resetPotionMixes()
        }
    }

    fun addPermissionRecipe(recipe: CustomRecipe, permission: String?) {
        permissionsPerRecipe[recipe] = permission ?: return
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
