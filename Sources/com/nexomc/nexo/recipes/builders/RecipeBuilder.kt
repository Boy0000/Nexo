package com.nexomc.nexo.recipes.builders

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.recipes.RecipeType
import com.nexomc.nexo.utils.NexoYaml.Companion.loadConfiguration
import com.nexomc.nexo.utils.printOnFailure
import com.nexomc.nexo.utils.resolve
import net.kyori.adventure.text.Component
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.*

@Suppress("UNCHECKED_CAST")
abstract class RecipeBuilder(
    val player: Player,
    private val recipeType: RecipeType,
    val inventoryTitle: Component = Component.text("${player.name} ${recipeType.id} builder")
) {
    var inventory: Inventory = currentBuilder(player.uniqueId)?.takeIf { it.recipeType == recipeType }?.inventory
        ?: createInventory(player, inventoryTitle)
    private set
    val configFile: File = NexoPlugin.instance().dataFolder.resolve("recipes/${recipeType.id}/${recipeType.id}_recipes.yml")
    val config: YamlConfiguration by lazy { loadConfiguration(configFile) }

    init {
        BUILDER_MAP[player.uniqueId] = this
    }

    abstract fun createInventory(player: Player?, inventoryTitle: Component): Inventory

    fun close() {
        BUILDER_MAP.remove(player.uniqueId)
    }

    abstract fun saveRecipe(name: String, permission: String?)

    protected fun setItemStack(section: ConfigurationSection, itemStack: ItemStack) {
        if (NexoItems.exists(itemStack)) section["nexo_item"] = NexoItems.idFromItem(itemStack)
        else if (itemStack.isSimilar(ItemStack(itemStack.type))) section["minecraft_type"] = itemStack.type.name
        else section["minecraft_item"] = itemStack

        if (itemStack.amount > 1) section["amount"] = itemStack.amount
    }

    fun saveConfig() {
        runCatching { config.save(configFile) }.printOnFailure()
    }

    fun setInventory(inventory: Inventory) {
        this.inventory = inventory
        BUILDER_MAP[player.uniqueId] = this
    }

    fun open() {
        inventory = currentBuilder(player.uniqueId)?.takeIf { it.recipeType == recipeType }?.inventory
            ?: createInventory(player, inventoryTitle)

        player.openInventory(inventory)
        BUILDER_MAP[player.uniqueId] = this
    }

    fun validSlot(slot: Int, slotType: InventoryType.SlotType): Boolean {
        return when (this) {
            is ShapedBuilder, is ShapelessBuilder, is CookingBuilder -> slotType == InventoryType.SlotType.RESULT
            is BrewingBuilder -> slot != 4 && slot != 1 && (slotType == InventoryType.SlotType.FUEL || slotType == InventoryType.SlotType.CRAFTING)
            else -> false
        }
    }

    companion object {
        private val BUILDER_MAP = mutableMapOf<UUID, RecipeBuilder>()

        @JvmStatic
        fun currentBuilder(playerUUID: UUID): RecipeBuilder? {
            return BUILDER_MAP[playerUUID]
        }

        @JvmStatic
        fun <T : RecipeBuilder> currentBuilder(playerUUID: UUID, builder: Class<T>): T? {
            return BUILDER_MAP[playerUUID]?.takeIf { it.javaClass == builder } as? T
        }
    }
}
