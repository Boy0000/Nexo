package com.nexomc.nexo.mechanics.farming.smelting

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.utils.BlockHelpers
import com.nexomc.nexo.utils.wrappers.EnchantmentWrapper
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.CookingRecipe
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import kotlin.random.Random

class SmeltingMechanicListener(private val factory: SmeltingMechanicFactory) : Listener {
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun BlockBreakEvent.onBlockBreak() {
        val item = player.inventory.itemInMainHand
        val location = BlockHelpers.toCenterLocation(block.location)
        val itemID = NexoItems.idFromItem(item) ?: return
        val mechanic = factory.getMechanic(itemID) ?: return

        if (block.drops.isEmpty() || player.gameMode == GameMode.CREATIVE || !item.hasItemMeta()) return

        val loot = furnace(block.drops.randomOrNull()) ?: return

        val itemMeta = item.itemMeta
        if ("ORE" in block.type.toString() && itemMeta.hasEnchant(EnchantmentWrapper.FORTUNE)) {
            loot.amount = 1 + Random.nextInt(itemMeta.getEnchantLevel(EnchantmentWrapper.FORTUNE))
        }

        isDropItems = false
        if (!location.isWorldLoaded()) return
        location.world.dropItemNaturally(location, loot)
        if (mechanic.playSound()) location.world.playSound(location, Sound.ENTITY_GUARDIAN_ATTACK, 0.10f, 0.8f)
    }

    private fun furnace(item: ItemStack?): ItemStack? {
        if (item == null) return null // Because item can be null

        val type = item.type.toString()
        if (type.startsWith("RAW_") && !type.endsWith("_BLOCK")) {
            item.type = Material.matchMaterial(item.type.toString().substring(4) + "_INGOT") ?: return null
            return item
        }

        for (recipe: Recipe in Bukkit.getRecipesFor(item)) {
            if (recipe !is CookingRecipe<*>) continue
            if (recipe.inputChoice.test(item)) return ItemStack(recipe.getResult().type, item.amount)
        }
        return null // return result furnace :)
    }
}
