package com.nexomc.nexo.mechanics.repair

import com.nexomc.nexo.mechanics.MechanicFactory
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

class RepairMechanicFactory(section: ConfigurationSection) : MechanicFactory(section) {
    val isOraxenDurabilityOnly: Boolean = section.getBoolean("nexo_durability_only")

    init {
        registerListeners(RepairMechanicListener(this))
    }

    override fun getMechanic(itemID: String?) = super.getMechanic(itemID) as? RepairMechanic
    override fun getMechanic(itemStack: ItemStack?) = super.getMechanic(itemStack) as? RepairMechanic
    override fun parse(mechanicSection: ConfigurationSection) = RepairMechanic(this, mechanicSection).apply(::addToImplemented)
}
