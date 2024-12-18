package com.nexomc.nexo.mechanics.farming.smelting

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.MechanicsManager
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

class SmeltingMechanicFactory(section: ConfigurationSection) : MechanicFactory(section) {
    init {
        instance = this
        MechanicsManager.registerListeners(NexoPlugin.instance(), mechanicID, SmeltingMechanicListener(this))
    }

    override fun parse(section: ConfigurationSection) = SmeltingMechanic(this, section)?.apply(::addToImplemented)

    override fun getMechanic(itemID: String?) = super.getMechanic(itemID) as? SmeltingMechanic?

    override fun getMechanic(itemStack: ItemStack?) = super.getMechanic(itemStack) as? SmeltingMechanic?

    companion object {
        private var instance: SmeltingMechanicFactory? = null

        fun instance(): SmeltingMechanicFactory? {
            return instance
        }
    }
}
