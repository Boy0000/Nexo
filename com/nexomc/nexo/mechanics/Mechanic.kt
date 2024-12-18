package com.nexomc.nexo.mechanics

import com.nexomc.nexo.items.ItemBuilder
import org.bukkit.configuration.ConfigurationSection
import java.util.function.Function

abstract class Mechanic @SafeVarargs protected constructor(
    val factory: MechanicFactory?, val section: ConfigurationSection,
    vararg modifiers: Function<ItemBuilder, ItemBuilder>
) {
    val itemModifiers = modifiers.toList().toTypedArray()
    val itemID = section.parent!!.parent!!.name
}
