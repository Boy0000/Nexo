package com.nexomc.nexo.mechanics.combat.spell.witherskull

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.MechanicsManager
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

class WitherSkullMechanicFactory(section: ConfigurationSection) : MechanicFactory(section) {
    init {
        MechanicsManager.registerListeners(NexoPlugin.instance(), mechanicID, WitherSkullMechanicListener(this))
    }

    override fun parse(section: ConfigurationSection): WitherSkullMechanic {
        val mechanic = WitherSkullMechanic(this, section)
        addToImplemented(mechanic)
        return mechanic
    }

    override fun getMechanic(itemID: String?) = super.getMechanic(itemID) as? WitherSkullMechanic

    override fun getMechanic(itemStack: ItemStack?) = super.getMechanic(itemStack) as? WitherSkullMechanic
}
