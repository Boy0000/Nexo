package com.nexomc.nexo.mechanics.misc.backpack

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.MechanicsManager
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

class BackpackMechanicFactory(section: ConfigurationSection) : MechanicFactory(section) {
    init {
        MechanicsManager.registerListeners(NexoPlugin.instance(), mechanicID, BackpackListener(this))
    }

    override fun parse(section: ConfigurationSection) = BackpackMechanic(this, section).apply(::addToImplemented)

    override fun getMechanic(itemID: String?) = super.getMechanic(itemID) as? BackpackMechanic

    override fun getMechanic(itemStack: ItemStack?) = super.getMechanic(itemStack) as? BackpackMechanic
}
