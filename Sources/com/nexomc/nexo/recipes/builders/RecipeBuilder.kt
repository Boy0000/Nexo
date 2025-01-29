package com.nexomc.nexo.recipes.builders

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.utils.NexoYaml.Companion.loadConfiguration
import com.nexomc.nexo.utils.printOnFailure
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.*
import kotlin.collections.set

abstract class RecipeBuilder protected constructor(val player: Player, private val builderName: String) {
    var inventory: Inventory
    private set
    private var configFile: File? = null
    private var config: YamlConfiguration? = null
    val inventoryTitle = "${player.name} $builderName builder"

    init {
        val uuid = player.uniqueId
        inventory = when {
            BUILDER_MAP.containsKey(uuid) && BUILDER_MAP[uuid]!!.builderName == builderName -> BUILDER_MAP[uuid]!!.inventory
            else -> createInventory(player, inventoryTitle)
        }
        player.openInventory(inventory)
        BUILDER_MAP[uuid] = this
    }

    abstract fun createInventory(player: Player?, inventoryTitle: String): Inventory

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

    fun getConfig(): YamlConfiguration? {
        if (configFile == null) {
            configFile = NexoPlugin.instance().resourceManager().extractConfiguration("recipes/$builderName.yml")
            config = loadConfiguration(configFile!!)
        }
        return config
    }

    fun saveConfig() {
        runCatching { config!!.save(configFile!!) }.printOnFailure()
    }

    fun setInventory(inventory: Inventory) {
        this.inventory = inventory
        BUILDER_MAP[player.uniqueId] = this
    }

    fun open() {
        player.openInventory(inventory)
    }

    companion object {
        private val BUILDER_MAP = mutableMapOf<UUID, RecipeBuilder>()

        @JvmStatic
        fun get(playerUUID: UUID): RecipeBuilder? {
            return BUILDER_MAP[playerUUID]
        }
    }
}
