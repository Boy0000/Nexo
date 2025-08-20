package com.nexomc.nexo.recipes.loaders

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.compatibilities.mythiccrucible.WrappedCrucibleItem
import com.nexomc.nexo.items.ItemUpdater
import com.nexomc.nexo.recipes.CustomRecipe
import com.nexomc.nexo.recipes.listeners.RecipeEventManager
import net.Indyuce.mmoitems.MMOItems
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.RecipeChoice

abstract class RecipeLoader protected constructor(protected val section: ConfigurationSection) {
    protected val result: ItemStack
        get() {
            val resultSection = section.getConfigurationSection("result") ?: return ItemStack(Material.AIR)
            val result: ItemStack?
            val amount = resultSection.getInt("amount", 1)

            when {
                resultSection.isString("nexo_item") -> result =
                    ItemUpdater.updateItem(NexoItems.itemFromId(resultSection.getString("nexo_item"))!!.build())
                resultSection.isString("crucible_item") -> result =
                    WrappedCrucibleItem(resultSection.getString("crucible_item")).build()
                resultSection.isString("mmoitems_id") && resultSection.isString("mmoitems_type") ->
                    result = MMOItems.plugin.getItem(resultSection.getString("mmoitems_type"), resultSection.getString("mmoitems_id"))
                resultSection.isString("minecraft_type") -> {
                    val material = Material.getMaterial(resultSection.getString("minecraft_type", "AIR")!!)
                    if (material == null || material.isAir) return ItemStack(Material.AIR)
                    result = ItemStack(material)
                }
                else -> result = resultSection.getItemStack("minecraft_item")
            }

            if (result != null) result.amount = amount
            return result ?: ItemStack(Material.AIR)
        }

    protected fun recipeChoice(iSection: ConfigurationSection): RecipeChoice? {
        when {
            iSection.isString("nexo_item") -> {
                val ingredient = NexoItems.itemFromId(iSection.getString("nexo_item"))!!.build()
                return RecipeChoice.ExactChoice(ItemUpdater.updateItem(ingredient))
            }
            iSection.isString("crucible_item") -> {
                val ingredient = WrappedCrucibleItem(iSection.getString("crucible_item")).build()
                return RecipeChoice.ExactChoice(ingredient ?: ItemStack(Material.AIR))
            }
            iSection.isString("mmoitems_id") && iSection.isString("mmoitems_type") -> {
                val ingredient = MMOItems.plugin.getItem(
                    iSection.getString("mmoitems_type"),
                    iSection.getString("mmoitems_id")
                ) ?: ItemStack(Material.AIR)
                return RecipeChoice.ExactChoice(ingredient)
            }
            else -> {
                iSection.getString("minecraft_type")?.let {
                    val material = Material.getMaterial(it)
                    if (material == null || material.isAir) return null
                    return RecipeChoice.MaterialChoice(material)
                }

                iSection.getString("tag")?.let {
                    val tagId = NamespacedKey.fromString(it) ?: NamespacedKey.minecraft("oak_logs")
                    val tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, tagId, Material::class.java) ?: Bukkit.getTag(Tag.REGISTRY_ITEMS, tagId, Material::class.java)
                    return tag?.let(RecipeChoice::MaterialChoice)
                }

                return RecipeChoice.ExactChoice(iSection.getItemStack("minecraft_item") ?: return null)
            }
        }
    }

    private val recipeName = section.name

    protected val key = NamespacedKey(NexoPlugin.instance(), recipeName)

    abstract fun registerRecipe()

    protected fun loadRecipe(recipe: Recipe?) {
        Bukkit.addRecipe(recipe, false)
        RecipeEventManager.instance().addPermissionRecipe(CustomRecipe.fromRecipe(recipe) ?: return, section.getString("permission"))
    }

    protected fun addToWhitelist(recipe: Recipe?) {
        RecipeEventManager.instance().whitelistRecipe(CustomRecipe.fromRecipe(recipe) ?: return)
    }
}
