package com.nexomc.nexo.mechanics.custom_block.noteblock.logstrip

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoItems
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

class LogStripping(logStripSection: ConfigurationSection) {
    private val _stripBlock: String? = logStripSection.getString("stripped_log")
    private val _logDrop: String? = logStripSection.getString("drop")
    private val decreaseAxeDurability: Boolean = logStripSection.getBoolean("decrease_axe_durability")
    val stripBlock by lazy { NexoBlocks.noteBlockMechanic(_stripBlock)?.blockData }
    val logDrop by lazy { NexoItems.itemFromId(_logDrop)?.build() ?: ItemStack(Material.AIR) }

    fun canBeStripped(): Boolean {
        return _stripBlock != null
    }

    fun hasStrippedDrop(): Boolean {
        return _logDrop != null
    }

    fun shouldDecreaseAxeDurability(): Boolean {
        return decreaseAxeDurability
    }
}
