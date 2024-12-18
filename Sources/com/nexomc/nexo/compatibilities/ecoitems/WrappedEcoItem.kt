package com.nexomc.nexo.compatibilities.ecoitems

import com.nexomc.nexo.utils.PluginUtils.isEnabled
import com.willfp.ecoitems.items.EcoItems.getByID
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

class WrappedEcoItem(private val id: String?) {

    constructor(section: ConfigurationSection) : this(section.getString("id"))

    fun build(): ItemStack? {
        if (id.isNullOrEmpty() || !isEnabled("EcoItems")) return null
        return getByID(id)?.itemStack
    }
}
