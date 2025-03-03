package com.nexomc.nexo.mechanics.repair

import com.nexomc.nexo.mechanics.Mechanic
import com.nexomc.nexo.mechanics.MechanicFactory
import org.bukkit.configuration.ConfigurationSection

class RepairMechanic(mechanicFactory: MechanicFactory, section: ConfigurationSection) : Mechanic(mechanicFactory, section) {
    private var ratio = section.getDouble("ratio", -1.0)
    private var fixedAmount = section.getInt("fixed_amount", -1)

    fun finalDamage(maxDurability: Int, damage: Int): Int {
        val amountToRepair = if (ratio != -1.0) (ratio * maxDurability).toInt() else fixedAmount
        return (damage - amountToRepair).coerceAtLeast(0)
    }
}
