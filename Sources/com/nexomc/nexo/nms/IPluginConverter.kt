package com.nexomc.nexo.nms

import org.bukkit.inventory.ItemStack

interface IPluginConverter {

    companion object {
        val armorPrefixRegex = "(helmet|chestplate|leggings|boots)".toRegex()
    }

    fun convertItemsAdder(itemStack: ItemStack)

    class EmptyPluginConverter : IPluginConverter {
        override fun convertItemsAdder(itemstack: ItemStack) {
        }
    }
}