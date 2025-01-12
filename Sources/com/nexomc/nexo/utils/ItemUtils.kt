package com.nexomc.nexo.utils

import com.nexomc.nexo.utils.EventUtils.call
import net.kyori.adventure.text.Component
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.*
import org.bukkit.inventory.meta.components.FoodComponent
import java.util.*
import java.util.function.Consumer
import kotlin.math.max

object ItemUtils {
    @JvmStatic
    fun isEmpty(itemStack: ItemStack?): Boolean {
        return itemStack == null || itemStack.type == Material.AIR || itemStack.amount == 0
    }

    fun subtract(itemStack: ItemStack, amount: Int) {
        itemStack.amount = max(0.0, (itemStack.amount - amount).toDouble()).toInt()
    }

    @JvmStatic
    fun itemName(itemMeta: ItemMeta?, otherMeta: ItemMeta) {
        if (itemMeta == null) return
        if (VersionUtil.isPaperServer) itemName(itemMeta, otherMeta.itemName())
        else itemMeta.setItemName(otherMeta.itemName)
    }

    fun itemName(itemMeta: ItemMeta, component: Component?) {
        val component = component?.takeUnless { it == Component.empty()}
        if (VersionUtil.isPaperServer) itemMeta.itemName(component)
        else itemMeta.setItemName(AdventureUtils.LEGACY_SERIALIZER.serialize(component!!))
    }

    @JvmStatic
    fun displayName(itemMeta: ItemMeta?, otherMeta: ItemMeta) {
        if (itemMeta == null) return
        if (VersionUtil.isPaperServer) displayName(itemMeta, otherMeta.displayName())
        else itemMeta.setDisplayName(otherMeta.displayName)
    }

    @JvmStatic
    fun displayName(itemStack: ItemStack, component: Component?) {
        editItemMeta(
            itemStack
        ) { itemMeta: ItemMeta ->
            if (VersionUtil.isPaperServer) itemMeta.displayName(component)
            else itemMeta.setDisplayName(
                Optional.ofNullable(component)
                    .map { component: Component? ->
                        AdventureUtils.LEGACY_SERIALIZER.serialize(
                            component!!
                        )
                    }.orElse(null)
            )
        }
    }

    @JvmStatic
    fun displayName(itemMeta: ItemMeta, component: Component?) {
        var component = component
        component = if (component !== Component.empty()) component else null
        if (VersionUtil.isPaperServer) itemMeta.displayName(component)
        else itemMeta.setDisplayName(AdventureUtils.LEGACY_SERIALIZER.serialize(component!!))
    }

    fun lore(itemStack: ItemStack, components: List<Component>) {
        if (VersionUtil.isPaperServer) itemStack.lore(components)
        else itemStack.lore = components.map(AdventureUtils.LEGACY_SERIALIZER::serialize)
    }

    fun lore(itemStack: ItemStack, strings: Collection<String>) {
        if (VersionUtil.isPaperServer) itemStack.lore(strings.map(AdventureUtils.MINI_MESSAGE::deserialize))
        else itemStack.lore = strings.toList()
    }

    fun lore(itemMeta: ItemMeta, components: List<Component>?) {
        if (VersionUtil.isPaperServer) itemMeta.lore(components)
        else itemMeta.lore = components?.map(AdventureUtils.LEGACY_SERIALIZER::serialize)
    }

    fun lore(itemMeta: ItemMeta, strings: Collection<String>) {
        if (VersionUtil.isPaperServer) itemMeta.lore((strings.map(AdventureUtils.MINI_MESSAGE::deserialize)))
        else itemMeta.lore = strings.toList()
    }

    @JvmStatic
    fun lore(itemMeta: ItemMeta, otherMeta: ItemMeta) {
        if (VersionUtil.isPaperServer) itemMeta.lore(otherMeta.lore())
        else itemMeta.lore = otherMeta.lore
    }

    @JvmStatic
    fun dyeColor(itemStack: ItemStack) = when (val meta = itemStack.itemMeta) {
        is LeatherArmorMeta -> meta.color
        is PotionMeta -> meta.color
        is MapMeta -> meta.color
        else -> null
    }.let { Optional.ofNullable(it) }

    @JvmStatic
    fun dyeItem(itemStack: ItemStack, color: Color?) {
        editItemMeta(itemStack) { meta: ItemMeta ->
            when (meta) {
                is LeatherArmorMeta -> meta.setColor(color)
                is PotionMeta -> meta.color = color
                is MapMeta -> meta.color = color
            }
        }
    }

    /**
     * @param itemStack The ItemStack to edit the ItemMeta of
     * @param function  The function-block to edit the ItemMeta in
     * @return The original ItemStack with the new ItemMeta
     */
    fun editItemMeta(itemStack: ItemStack, function: Consumer<ItemMeta>) {
        val meta = itemStack.itemMeta ?: return
        function.accept(meta)
        itemStack.itemMeta = meta
    }

    /**
     * Used to correctly damage the item in the player's hand based on broken block
     * Only handles it if the block is a NexoBlock or NexoFurniture
     *
     * @param player    the player that broke the NexoBlock or NexoFurniture
     * @param itemStack the item in the player's hand
     */
    @JvmStatic
    fun damageItem(player: Player, itemStack: ItemStack) {
        // If the item is not a tool, it will not be damaged, example flint&steel should not be damaged
        val damage = if (isTool(itemStack)) 1 else 0

        if (damage == 0) return
        if (VersionUtil.isPaperServer) player.damageItemStack(itemStack, damage)
        else editItemMeta(itemStack) { meta ->
            if (meta is Damageable && PlayerItemDamageEvent(player, itemStack, damage).call())
                meta.damage += 1
        }
    }

    @JvmStatic
    fun isTool(itemStack: ItemStack): Boolean {
        return isTool(itemStack.type)
    }

    fun isTool(material: Material): Boolean {
        return material.toString().endsWith("_AXE")
                || material.toString().endsWith("_PICKAXE")
                || material.toString().endsWith("_SHOVEL")
                || material.toString().endsWith("_HOE")
                || material.toString().endsWith("_SWORD")
                || material == Material.TRIDENT
    }

    @JvmStatic
    fun isMusicDisc(itemStack: ItemStack): Boolean {
        if (VersionUtil.atleast("1.21")) return itemStack.hasItemMeta() && itemStack.itemMeta.hasJukeboxPlayable()
        return itemStack.type.name.startsWith("MUSIC_DISC")
    }

    @JvmStatic
    fun getUsingConvertsTo(itemMeta: ItemMeta): ItemStack? {
        if (VersionUtil.below("1.21")) return null
        if (VersionUtil.atleast("1.21.2")) return if (itemMeta.hasUseRemainder()) itemMeta.useRemainder else null

        return runCatching {
            FoodComponent::class.java.getMethod("getUsingConvertsTo").invoke(itemMeta.food) as? ItemStack
        }.onFailure { it.printStackTrace() }.getOrNull()
    }

    @JvmStatic
    fun setUsingConvertsTo(foodComponent: FoodComponent, replacement: ItemStack?) {
        if (!VersionUtil.matchesServer("1.21.1")) return
        runCatching {
            FoodComponent::class.java.getMethod("setUsingConvertsTo", ItemStack::class.java).invoke(foodComponent, replacement)
        }.onFailure { it.printStackTrace() }
    }
}
