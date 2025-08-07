package com.nexomc.nexo.mechanics.misc.backpack

import com.nexomc.nexo.api.NexoItems
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

data class BackpackBlacklist(val nexoItems: List<String> = listOf(), val materials: List<Material> = listOf()) {

    constructor(section: ConfigurationSection) : this(
        section.getStringList("nexo_items"),
        section.getStringList("materials").mapNotNull(Material::getMaterial)
    )

    operator fun contains(item: ItemStack): Boolean {
        return item.type in materials || NexoItems.idFromItem(item) in nexoItems
    }
}