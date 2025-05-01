package com.nexomc.nexo.mechanics.combat.spell.fireball

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.MechanicsManager
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

class FireballMechanicFactory(section: ConfigurationSection) : MechanicFactory(section) {
    init {
        MechanicsManager.registerListeners(NexoPlugin.instance(), mechanicID, FireballMechanicListener(this))
    }

    override fun parse(section: ConfigurationSection) = FireballMechanic(this, section).apply(::addToImplemented)

    override fun getMechanic(itemID: String?) = super.getMechanic(itemID) as? FireballMechanic

    override fun getMechanic(itemStack: ItemStack?) = super.getMechanic(itemStack) as? FireballMechanic
}
