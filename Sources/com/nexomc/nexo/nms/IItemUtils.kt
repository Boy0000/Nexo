package com.nexomc.nexo.nms

import com.nexomc.nexo.items.ItemBuilder
import com.nexomc.nexo.utils.Colorable
import org.bukkit.Art
import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.FireworkEffectMeta
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.MapMeta
import org.bukkit.inventory.meta.PotionMeta

interface IItemUtils {

    fun asColorable(itemStack: ItemStack): Colorable?

    fun paintingVariant(itemStack: ItemStack): Art? { return null }

    fun paintingVariant(itemStack: ItemStack, paintingVariant: Art?) {}

    fun foodComponent(itemBuilder: ItemBuilder, foodSection: ConfigurationSection) {}

    fun consumableComponent(itemStack: ItemStack?): Any? {
        return null
    }

    fun consumableComponent(itemBuilder: ItemBuilder, consumableSection: ConfigurationSection) {}

    fun consumableComponent(itemStack: ItemStack, consumable: Any?) {}

    fun repairableComponent(itemStack: ItemStack?): Any? {
        return null
    }

    fun repairableComponent(itemBuilder: ItemBuilder, repairableWith: List<String>) {}

    fun repairableComponent(itemStack: ItemStack, repairable: Any?) {}

    fun blockstateComponent(itemStack: ItemStack?): Map<String, String>? {
        return null
    }

    fun blockstateComponent(itemStack: ItemStack, blockstates: Map<String, String>?) {}

    fun handleItemFlagToolTips(itemStack: ItemStack, itemFlags: Set<ItemFlag>) {}

    /**
     * Copies over all NBT-Tags from oldItem to newItem
     * Useful for plugins that might register their own NBT-Tags outside
     * the ItemStacks PersistentDataContainer
     *
     * @param oldItem The old ItemStack to copy the NBT-Tags from
     * @param newItem The new ItemStack to copy the NBT-Tags to
     * @return The new ItemStack with the copied NBT-Tags
     */
    fun copyItemNBTTags(oldItem: ItemStack, newItem: ItemStack): ItemStack

    companion object {
        /**
         * Keys that are used by vanilla Minecraft and should therefore be skipped
         * Some are accessed through API methods, others are just used internally
         */
        @JvmField
        val vanillaKeys = setOf(
            "PublicBukkitValues",
            "display",
            "CustomModelData",
            "Damage",
            "AttributeModifiers",
            "Unbreakable",
            "CanDestroy",
            "slot",
            "count",
            "HideFlags",
            "CanPlaceOn",
            "Enchantments",
            "StoredEnchantments",
            "RepairCost",
            "CustomPotionEffects",
            "Potion",
            "CustomPotionColor",
            "Trim",
            "EntityTag",
            "pages",
            "filtered_pages",
            "filtered_title",
            "resolved",
            "generation",
            "author",
            "title",
            "BucketVariantTag",
            "Items",
            "LodestoneTracked",
            "LodestoneDimension",
            "LodestonePos",
            "ChargedProjectiles",
            "Charged",
            "DebugProperty",
            "Fireworks",
            "Explosion",
            "Flight",
            "map",
            "map_scale_direction",
            "map_to_lock",
            "Decorations",
            "SkullOwner",
            "Effects",
            "BlockEntityTag",
            "BlockStateTag"
        )
    }

    class EmptyItemUtils : IItemUtils {
        override fun asColorable(itemStack: ItemStack): Colorable? {
            return when (val meta = itemStack.itemMeta) {
                is LeatherArmorMeta -> object : Colorable {
                    override var color: Color?
                        get() = meta.color
                        set(value) {
                            meta.setColor(value)
                            itemStack.setItemMeta(meta)
                        }
                }

                is PotionMeta -> object : Colorable {
                    override var color: Color?
                        get() = meta.color
                        set(value) {
                            meta.color = value
                            itemStack.setItemMeta(meta)
                        }
                }

                is MapMeta -> object : Colorable {
                    override var color: Color?
                        get() = meta.color
                        set(value) {
                            meta.color = value
                            itemStack.setItemMeta(meta)
                        }
                }

                is FireworkEffectMeta -> object : Colorable {
                    override var color: Color?
                        get() = meta.effect?.colors?.firstOrNull()
                        set(value) {
                            meta.effect = FireworkEffect.builder()
                                .withColor(setOf(value ?: meta.effect?.colors ?: listOf(Color.GRAY)))
                                .with(meta.effect?.type ?: FireworkEffect.Type.BALL)
                                .withFade(meta.effect?.fadeColors ?: emptyList<Color>())
                                .trail(meta.effect?.hasTrail() ?: false)
                                .flicker(meta.effect?.hasFlicker() ?: false)
                                .build()
                            itemStack.setItemMeta(meta)
                        }
                }

                else -> null
            }
        }
        override fun copyItemNBTTags(oldItem: ItemStack, newItem: ItemStack) = newItem
    }
}