package com.nexomc.nexo.mechanics.farming.smelting

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.utils.BlockHelpers
import com.nexomc.nexo.utils.wrappers.EnchantmentWrapper
import org.bukkit.*
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
        val location = BlockHelpers.toCenterLocation(block.location).takeIf(Location::isWorldLoaded) ?: return
        val mechanic = factory.getMechanic(item) ?: return

        if (block.drops.isEmpty() || player.gameMode == GameMode.CREATIVE || !item.hasItemMeta()) return

        val loot = furnace(block.drops.randomOrNull()) ?: return

        val itemMeta = item.itemMeta
        if ("ORE" in block.type.toString() && itemMeta.hasEnchant(EnchantmentWrapper.FORTUNE)) {
            loot.amount = 1 + Random.nextInt(itemMeta.getEnchantLevel(EnchantmentWrapper.FORTUNE))
        }

        isDropItems = false
        location.world.dropItemNaturally(location, loot)
        if (mechanic.playSound()) location.world.playSound(location, Sound.ENTITY_GUARDIAN_ATTACK, 0.10f, 0.8f)
    }

    private fun furnace(item: ItemStack?): ItemStack? {
        val type = item?.type?.toString() ?: return null
        if (type.startsWith("RAW_") && !type.endsWith("_BLOCK")) {
            item.type = Material.matchMaterial(type.substring(4) + "_INGOT") ?: return null
            return item
        }

        return Bukkit.getRecipesFor(item).filterIsInstance<CookingRecipe<*>>().firstOrNull { recipe ->
            recipe.inputChoice.test(item)
        }?.let { ItemStack(it.result.type, item.amount) }
    }
}
