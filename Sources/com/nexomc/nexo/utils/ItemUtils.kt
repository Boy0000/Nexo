package com.nexomc.nexo.utils

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.components.FoodComponent
import org.bukkit.persistence.PersistentDataContainer

inline fun <reified T : ItemMeta> ItemStack.editMeta(crossinline block: (T) -> Boolean): ItemStack {
    val meta = itemMeta as? T ?: return this
    val hasChanged = block(meta)
    if (hasChanged) itemMeta = meta
    return this
}

object ItemUtils {

    val ItemStack.persistentDataView: PersistentDataContainer? get() = runCatching {
        persistentDataContainer as PersistentDataContainer
    }.getOrDefault(itemMeta?.persistentDataContainer)

    fun editPersistentDataContainer(itemStack: ItemStack, action: (PersistentDataContainer) -> Unit) {
        runCatching {
            itemStack.editPersistentDataContainer { action.invoke(it) }
        }.onFailure {
            itemStack.editMeta {
                action.invoke(it.persistentDataContainer)
            }
        }
    }

    fun itemStacks(vararg materials: Material): List<ItemStack> {
        return materials.map(::ItemStack)
    }

    fun triggerCooldown(player: Player, item: ItemStack) {
        if (!VersionUtil.atleast("1.21.2")) return
        val cooldown = item.itemMeta?.takeIf { it.hasUseCooldown() }?.useCooldown ?: return
        val cooldownTime = cooldown.cooldownSeconds.times(20).toInt()

        player.setCooldown(item, cooldownTime)
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
