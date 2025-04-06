package com.nexomc.nexo.mechanics

import com.nexomc.nexo.items.ItemBuilder
import java.util.function.Function
import org.bukkit.configuration.ConfigurationSection

abstract class Mechanic @SafeVarargs protected constructor(
    val factory: MechanicFactory?, val section: ConfigurationSection,
    vararg modifiers: Function<ItemBuilder, ItemBuilder>
) {
    val itemModifiers = modifiers
    val itemID = section.parent!!.parent!!.name
}
