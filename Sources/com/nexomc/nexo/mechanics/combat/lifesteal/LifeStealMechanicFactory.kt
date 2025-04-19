package com.nexomc.nexo.mechanics.combat.lifesteal

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.MechanicsManager
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

class LifeStealMechanicFactory(section: ConfigurationSection) : MechanicFactory(section) {
    init {
        MechanicsManager.registerListeners(NexoPlugin.instance(), mechanicID, LifeStealMechanicListener(this))
    }

    override fun parse(section: ConfigurationSection): LifeStealMechanic {
        val mechanic = LifeStealMechanic(this, section)
        addToImplemented(mechanic)
        return mechanic
    }

    override fun getMechanic(itemID: String?) = super.getMechanic(itemID) as? LifeStealMechanic

    override fun getMechanic(itemStack: ItemStack?) = super.getMechanic(itemStack) as? LifeStealMechanic
}
