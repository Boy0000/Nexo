package com.nexomc.nexo.mechanics.misc.itemtype

import com.nexomc.nexo.mechanics.Mechanic
import com.nexomc.nexo.mechanics.MechanicFactory
import org.bukkit.configuration.ConfigurationSection

class ItemTypeMechanic(mechanicFactory: MechanicFactory?, section: ConfigurationSection) : Mechanic(mechanicFactory, section) {
    val itemType = section.getString("value")
}
