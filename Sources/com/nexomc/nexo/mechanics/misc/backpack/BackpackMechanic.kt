package com.nexomc.nexo.mechanics.misc.backpack

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.mechanics.Mechanic
import com.nexomc.nexo.mechanics.MechanicFactory
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

class BackpackMechanic(factory: MechanicFactory, section: ConfigurationSection) : Mechanic(factory, section) {
    val rows = section.getInt("rows", 6)
    val title = section.getString("title", "Backpack")!!
    val openSound = section.getString("open_sound", "minecraft:entity.shulker.open")!!
    val closeSound = section.getString("close_sound", "minecraft:entity.shulker.close")!!
    val volume = section.getDouble("volume", 1.0).toFloat()
    val pitch = section.getDouble("pitch", 1.0).toFloat()
    val blacklist = section.getConfigurationSection("blacklist")?.let(::BackpackBlacklist) ?: BackpackBlacklist()

    companion object {
        val BACKPACK_KEY = NamespacedKey(NexoPlugin.instance(), "backpack")
    }

    fun isBlacklisted(item: ItemStack?): Boolean {
        return item != null && blacklist.contains(item)
    }
}
