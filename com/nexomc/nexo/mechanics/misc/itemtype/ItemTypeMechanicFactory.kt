package com.nexomc.nexo.mechanics.misc.itemtype

import com.nexomc.nexo.mechanics.MechanicFactory
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

class ItemTypeMechanicFactory(section: ConfigurationSection) : MechanicFactory(section) {
    init {
        instance = this
    }

    override fun parse(section: ConfigurationSection): ItemTypeMechanic {
        val mechanic = ItemTypeMechanic(this, section)
        addToImplemented(mechanic)
        return mechanic
    }

    override fun getMechanic(itemID: String?) = super.getMechanic(itemID) as? ItemTypeMechanic

    override fun getMechanic(itemStack: ItemStack?) = super.getMechanic(itemStack) as? ItemTypeMechanic

    companion object {
        private lateinit var instance: ItemTypeMechanicFactory
        fun get(): ItemTypeMechanicFactory {
            return instance
        }
    }
}
