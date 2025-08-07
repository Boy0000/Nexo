package com.nexomc.nexo.mechanics

import com.nexomc.nexo.items.ItemBuilder
import com.nexomc.nexo.utils.rootId
import org.bukkit.configuration.ConfigurationSection
import java.util.function.Function

abstract class Mechanic @SafeVarargs protected constructor(
    val factory: MechanicFactory?, val section: ConfigurationSection,
    vararg modifiers: Function<ItemBuilder, ItemBuilder>
) {
    val itemModifiers = modifiers
    val itemID = section.rootId
}
