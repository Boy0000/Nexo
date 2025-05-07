package com.nexomc.nexo.mechanics.trident

import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.utils.safeCast
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

class TridentFactory(section: ConfigurationSection) : MechanicFactory(section) {
    init {
        registerListeners(CustomTridentListener())
        instance = this
    }

    override fun getMechanic(itemId: String?) = super.getMechanic(itemId).safeCast<TridentMechanic>()
    override fun getMechanic(itemStack: ItemStack?) = super.getMechanic(itemStack).safeCast<TridentMechanic>()
    override fun parse(section: ConfigurationSection) = TridentMechanic(this, section).apply(::addToImplemented)

    companion object {
        private var instance: TridentFactory? = null
        fun instance() = instance
    }
}