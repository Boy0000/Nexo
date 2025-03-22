package com.nexomc.nexo.mechanics.custom_block.noteblock.logstrip

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoItems
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

class LogStripping(logStripSection: ConfigurationSection) {
    val decreaseAxeDurability: Boolean = logStripSection.getBoolean("decrease_axe_durability")
    val stripMechanic by lazy { NexoBlocks.noteBlockMechanic(logStripSection.getString("stripped_log")) }
    val stripBlock by lazy { stripMechanic?.blockData }
    val logDrop by lazy { NexoItems.itemFromId(logStripSection.getString("drop"))?.build() ?: ItemStack(Material.AIR) }

    fun canBeStripped(): Boolean {
        return stripBlock != null
    }

    fun hasStrippedDrop(): Boolean {
        return !logDrop.isEmpty
    }
}
