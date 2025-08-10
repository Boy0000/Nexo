package com.nexomc.nexo.mechanics.combat.spell

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.items.ItemBuilder
import com.nexomc.nexo.mechanics.Mechanic
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.utils.timers.TimersFactory
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

abstract class SpellMechanic protected constructor(factory: MechanicFactory, section: ConfigurationSection) : Mechanic(
    factory, section, { item: ItemBuilder ->
        val initCharges = section.getInt("charges", -1)
        item.customTag<Int, Int>(NAMESPACED_KEY, PersistentDataType.INTEGER, initCharges)
    },
    func@{ item: ItemBuilder ->
        if (section.getInt("charges", -1) == -1) return@func item

        val lore = item.lore().toMutableList()
        lore.addFirst(Component.text("Charges ${section.getInt("charges")}/${section.getInt("charges")}"))
        item.lore(lore)
    }
) {
    val charges: Int = section.getInt("charges", -1)
    private val timersFactory: TimersFactory = TimersFactory(section.getLong("delay"))

    fun maxCharges() = this.charges

    open fun timer(player: Player) = timersFactory.getTimer(player)

    fun removeCharge(item: ItemStack) {
        item.editMeta { itemMeta: ItemMeta ->
            val pdc = itemMeta.persistentDataContainer
            if (!pdc.has(NAMESPACED_KEY, PersistentDataType.INTEGER)) return@editMeta

            val chargesLeft = pdc.getOrDefault(NAMESPACED_KEY, PersistentDataType.INTEGER, -1)
            if (chargesLeft == -1) return@editMeta
            if (chargesLeft == 1) item.amount = 0
            else {
                pdc.set(NAMESPACED_KEY, PersistentDataType.INTEGER, chargesLeft - 1)

                itemMeta.lore((itemMeta.lore() ?: mutableListOf()).also {
                    it.addFirst(Component.text("Charges ${(chargesLeft - 1)}/${this.maxCharges()}"))
                })
            }
        }
    }

    companion object {
        val NAMESPACED_KEY = NamespacedKey(NexoPlugin.instance(), "charges")
    }
}
