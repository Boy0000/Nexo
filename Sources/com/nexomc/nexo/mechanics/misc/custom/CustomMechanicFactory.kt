package com.nexomc.nexo.mechanics.misc.custom

import com.nexomc.nexo.mechanics.MechanicFactory
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

class CustomMechanicFactory(section: ConfigurationSection) : MechanicFactory(section) {
    override fun parse(section: ConfigurationSection) = CustomMechanic(this, section).apply(::addToImplemented)

    override fun getMechanic(itemID: String?) = super.getMechanic(itemID) as? CustomMechanic

    override fun getMechanic(itemStack: ItemStack?) = super.getMechanic(itemStack) as? CustomMechanic
}
