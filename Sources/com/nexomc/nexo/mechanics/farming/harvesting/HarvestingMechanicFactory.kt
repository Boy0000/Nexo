package com.nexomc.nexo.mechanics.farming.harvesting

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.MechanicsManager
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

class HarvestingMechanicFactory(section: ConfigurationSection) : MechanicFactory(section) {
    init {
        instance = this
        MechanicsManager.registerListeners(NexoPlugin.instance(), mechanicID, HarvestingMechanicListener())
    }

    override fun parse(section: ConfigurationSection): HarvestingMechanic {
        val mechanic = HarvestingMechanic(this, section)
        addToImplemented(mechanic)
        return mechanic
    }

    override fun getMechanic(itemID: String?) = super.getMechanic(itemID) as? HarvestingMechanic

    override fun getMechanic(itemStack: ItemStack?) = super.getMechanic(itemStack) as? HarvestingMechanic

    companion object {
        private var instance: HarvestingMechanicFactory? = null

        fun instance(): HarvestingMechanicFactory? {
            return instance
        }
    }
}
