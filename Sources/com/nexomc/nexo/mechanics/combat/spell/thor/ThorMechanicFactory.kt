package com.nexomc.nexo.mechanics.combat.spell.thor

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.MechanicsManager
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

class ThorMechanicFactory(section: ConfigurationSection) : MechanicFactory(section) {
    init {
        MechanicsManager.registerListeners(NexoPlugin.instance(), mechanicID, ThorMechanicListener(this))
    }

    override fun parse(section: ConfigurationSection) = ThorMechanic(this, section).apply(::addToImplemented)

    override fun getMechanic(itemID: String?) = super.getMechanic(itemID) as? ThorMechanic

    override fun getMechanic(itemStack: ItemStack?) = super.getMechanic(itemStack) as? ThorMechanic
}
