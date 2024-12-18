package com.nexomc.nexo.mechanics.combat.spell.energyblast

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.MechanicsManager
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

class EnergyBlastMechanicFactory(section: ConfigurationSection) : MechanicFactory(section) {
    init {
        MechanicsManager.registerListeners(NexoPlugin.instance(), mechanicID, EnergyBlastMechanicManager(this))
    }

    override fun parse(section: ConfigurationSection) = EnergyBlastMechanic(this, section).apply(::addToImplemented)

    override fun getMechanic(itemID: String?) = super.getMechanic(itemID) as? EnergyBlastMechanic?

    override fun getMechanic(itemStack: ItemStack?) = super.getMechanic(itemStack) as? EnergyBlastMechanic?
}
