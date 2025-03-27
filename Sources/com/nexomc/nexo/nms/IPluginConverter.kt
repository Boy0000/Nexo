package com.nexomc.nexo.nms

import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack

interface IPluginConverter {

    fun convertItemsAdder(itemStack: ItemStack)
    fun convertOraxen(itemStack: ItemStack)
    fun convertOraxen(entity: Entity)

    class EmptyPluginConverter : IPluginConverter {
        override fun convertItemsAdder(itemStack: ItemStack) {
        }
        override fun convertOraxen(itemStack: ItemStack) {
        }
        override fun convertOraxen(entity: Entity) {
        }
    }
}