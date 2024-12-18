package com.nexomc.nexo.mechanics

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoItems
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack

abstract class MechanicFactory {
    private val mechanicByItemId = mutableMapOf<String, Mechanic>()
    val mechanicID: String
    private val section: ConfigurationSection?

    protected constructor(section: ConfigurationSection) {
        this.section = section
        this.mechanicID = section.name
    }

    protected constructor(mechanicId: String) {
        this.mechanicID = mechanicId
        this.section = null
    }

    protected fun section() = this.section

    abstract fun parse(section: ConfigurationSection): Mechanic?

    protected fun addToImplemented(mechanic: Mechanic?) {
        if (mechanic == null) return
        mechanicByItemId[mechanic.itemID] = mechanic
    }

    fun items() = mechanicByItemId.keys.toSet()

    fun isNotImplementedIn(itemID: String?) = !mechanicByItemId.containsKey(itemID)

    fun isNotImplementedIn(itemStack: ItemStack) = !mechanicByItemId.containsKey(NexoItems.idFromItem(itemStack))

    open fun getMechanic(itemID: String?) = mechanicByItemId[itemID]

    open fun getMechanic(itemStack: ItemStack?) = mechanicByItemId[NexoItems.idFromItem(itemStack)]

    fun registerListeners(vararg listeners: Listener) {
        MechanicsManager.registerListeners(NexoPlugin.instance(), mechanicID, *listeners)
    }
}
