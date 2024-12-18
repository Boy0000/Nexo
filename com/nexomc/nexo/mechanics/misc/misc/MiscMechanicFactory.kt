package com.nexomc.nexo.mechanics.misc.misc

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.MechanicsManager
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.logs.Logs
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

class MiscMechanicFactory(section: ConfigurationSection) : MechanicFactory(section) {
    init {
        MechanicsManager.registerListeners(NexoPlugin.instance(), mechanicID, MiscListener())
        instance = this
    }

    override fun parse(section: ConfigurationSection): MiscMechanic {
        val mechanic = MiscMechanic(this, section)

        if (VersionUtil.atleast("1.21.2")) {
            if (!mechanic.burnsInLava() || !mechanic.burnsInLava()) {
                Logs.logWarn(mechanic.itemID + " is using deprecated Misc-Mechanic burns_in_fire/lava...")
                Logs.logWarn("It is heavily advised to swap to the new `damage_resistant`-component on 1.21.2+ servers...")
            } else if (!mechanic.breaksFromCactus()) {
                Logs.logWarn(mechanic.itemID + " is using deprecated Misc-Mechanic breaks_from_cactus...")
                Logs.logWarn("It is heavily advised to swap to the new `damage_resistant`-component on 1.21.2+ servers...")
            }
        }

        addToImplemented(mechanic)
        return mechanic
    }

    override fun getMechanic(itemID: String?) = super.getMechanic(itemID) as? MiscMechanic

    override fun getMechanic(itemStack: ItemStack?) = super.getMechanic(itemStack) as? MiscMechanic

    companion object {
        private var instance: MiscMechanicFactory? = null
        fun instance() = instance
    }
}
