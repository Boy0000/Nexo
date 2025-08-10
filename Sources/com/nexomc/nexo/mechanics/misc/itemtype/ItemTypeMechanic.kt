package com.nexomc.nexo.mechanics.misc.itemtype

import com.nexomc.nexo.mechanics.Mechanic
import org.bukkit.configuration.ConfigurationSection

class ItemTypeMechanic(mechanicFactory: ItemTypeMechanicFactory, section: ConfigurationSection) : Mechanic(mechanicFactory, section) {
    val itemType = section.getString("value")
}
