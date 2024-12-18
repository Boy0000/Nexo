package com.nexomc.nexo.mechanics.misc.soulbound

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.MechanicsManager
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

class SoulBoundMechanicFactory(section: ConfigurationSection) : MechanicFactory(section) {
    init {
        MechanicsManager.registerListeners(NexoPlugin.instance(), mechanicID, SoulBoundMechanicListener(this))
    }

    override fun parse(section: ConfigurationSection) = SoulBoundMechanic(this, section).apply(::addToImplemented)

    override fun getMechanic(itemID: String?) = super.getMechanic(itemID) as? SoulBoundMechanic

    override fun getMechanic(itemStack: ItemStack?) = super.getMechanic(itemStack) as? SoulBoundMechanic
}
