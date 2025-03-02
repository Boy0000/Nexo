package com.nexomc.nexo.utils

import com.mineinabyss.idofront.items.asColorable
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
import javax.annotation.Nullable
import kotlin.math.max

object ItemUtils {

    fun itemStacks(vararg materials: Material): List<ItemStack> {
        return materials.map(::ItemStack)
    }

    @JvmStatic
    fun isEmpty(itemStack: ItemStack?): Boolean {
        return itemStack == null || itemStack.type == Material.AIR || itemStack.amount == 0
    }

    @JvmStatic
    @Nullable
    fun dyeColor(itemStack: ItemStack) = itemStack.itemMeta.asColorable()?.color

    @JvmStatic
    fun dyeItem(itemStack: ItemStack, color: Color?) {
        itemStack.editMeta { it.asColorable()?.color = color }
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
        if (isTool(itemStack)) player.damageItemStack(itemStack, 1)
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
