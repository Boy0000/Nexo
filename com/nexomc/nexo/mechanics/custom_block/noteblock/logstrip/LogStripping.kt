package com.nexomc.nexo.mechanics.custom_block.noteblock.logstrip

import org.bukkit.configuration.ConfigurationSection

class LogStripping(logStripSection: ConfigurationSection) {
    val strippedLogBlock: String?
    val strippedLogDrop: String?
    private val decreaseAxeDurability: Boolean

    init {
        strippedLogBlock = logStripSection.getString("stripped_log")
        strippedLogDrop = logStripSection.getString("drop")
        decreaseAxeDurability = logStripSection.getBoolean("decrease_axe_durability")
    }

    fun canBeStripped(): Boolean {
        return strippedLogBlock != null
    }

    fun hasStrippedDrop(): Boolean {
        return strippedLogDrop != null
    }

    fun shouldDecreaseAxeDurability(): Boolean {
        return decreaseAxeDurability
    }
}
