package com.nexomc.nexo.recipes.loaders

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.compatibilities.ecoitems.WrappedEcoItem
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
                resultSection.isString("ecoitem_id") -> result =
                    WrappedEcoItem(resultSection.getString("ecoitem_id")).build()
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

    protected fun recipeChoice(ingredientSection: ConfigurationSection): RecipeChoice? {
        when {
            ingredientSection.isString("nexo_item") ->
                return RecipeChoice.ExactChoice(ItemUpdater.updateItem(NexoItems.itemFromId(ingredientSection.getString("nexo_item"))!!.build()))
            ingredientSection.isString("crucible_item") -> {
                val ingredient = WrappedCrucibleItem(section.getString("crucible_item")).build()
                return RecipeChoice.ExactChoice(ingredient ?: ItemStack(Material.AIR))
            }
            ingredientSection.isString("mmoitems_id") && ingredientSection.isString("mmoitems_type") -> {
                val ingredient = MMOItems.plugin.getItem(
                    ingredientSection.getString("mmoitems_type"),
                    ingredientSection.getString("mmoitems_id")
                )
                return RecipeChoice.ExactChoice(
                    ingredient
                        ?: ItemStack(Material.AIR)
                )
            }
            ingredientSection.isString("ecoitem_id") -> {
                val ingredient = WrappedEcoItem(section.getString("ecoitem_id")).build()
                return RecipeChoice.ExactChoice(ingredient ?: ItemStack(Material.AIR))
            }
            else -> {
                ingredientSection.getString("minecraft_type")?.let {
                    val material = Material.getMaterial(it)
                    if (material == null || material.isAir) return null
                    return RecipeChoice.MaterialChoice(material)
                }

                ingredientSection.getString("tag")?.let {
                    val tagId = NamespacedKey.fromString(it) ?: NamespacedKey.minecraft("oak_logs")
                    val tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, tagId, Material::class.java) ?: Bukkit.getTag(Tag.REGISTRY_ITEMS, tagId, Material::class.java)
                    return tag?.let(RecipeChoice::MaterialChoice)
                }

                return RecipeChoice.ExactChoice(ingredientSection.getItemStack("minecraft_item") ?: return null)
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
