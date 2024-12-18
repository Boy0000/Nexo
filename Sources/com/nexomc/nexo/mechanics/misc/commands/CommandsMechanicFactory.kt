package com.nexomc.nexo.mechanics.misc.commands

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.MechanicsManager
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

class CommandsMechanicFactory(section: ConfigurationSection) : MechanicFactory(section) {
    init {
        MechanicsManager.registerListeners(NexoPlugin.instance(), mechanicID, CommandsMechanicListener(this))
    }

    override fun parse(section: ConfigurationSection) = CommandsMechanic(this, section).apply(::addToImplemented)

    override fun getMechanic(itemID: String?) = super.getMechanic(itemID) as? CommandsMechanic

    override fun getMechanic(itemStack: ItemStack?) = super.getMechanic(itemStack) as? CommandsMechanic
}
