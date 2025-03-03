package com.nexomc.nexo.mechanics.misc.armor_effects

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.MechanicsManager
import com.tcoded.folialib.wrapper.task.WrappedBukkitTask
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack

class ArmorEffectsFactory(section: ConfigurationSection) : MechanicFactory(section) {
    private var armorEffectTask: ArmorEffectsTask? = null
    val delay = section.getInt("delay_in_ticks", 20)

    init {
        instance = this
        registerListeners(object : Listener {
            @EventHandler(priority = EventPriority.HIGHEST)
            fun PlayerArmorChangeEvent.onItemEquipped() {
                ArmorEffectsMechanic.addEffects(player)
            }
        })
    }

    override fun parse(section: ConfigurationSection): ArmorEffectsMechanic {
        val mechanic = ArmorEffectsMechanic(this, section).apply(::addToImplemented)

        armorEffectTask?.cancel()
        armorEffectTask = ArmorEffectsTask()
        MechanicsManager.registerTask(
            instance.mechanicID,
            WrappedBukkitTask(armorEffectTask!!.runTaskTimer(NexoPlugin.instance(), 0, delay.toLong()))
        )

        return mechanic
    }

    override fun getMechanic(itemID: String?) = super.getMechanic(itemID) as? ArmorEffectsMechanic

    override fun getMechanic(itemStack: ItemStack?) = super.getMechanic(itemStack) as? ArmorEffectsMechanic

    companion object {
        private lateinit var instance: ArmorEffectsFactory
        fun instance(): ArmorEffectsFactory {
            return instance
        }
    }
}
