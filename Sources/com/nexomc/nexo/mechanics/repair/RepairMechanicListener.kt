package com.nexomc.nexo.mechanics.repair

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.meta.Damageable

class RepairMechanicListener(val factory: RepairMechanicFactory) : Listener {

    @EventHandler
    fun InventoryClickEvent.onRepairItem() {
        val repairMechanic = factory.takeUnless { it.isNexoDurabilityOnly }?.getMechanic(cursor) ?: return
        val toRepair = currentItem ?: return
        val toRepairMeta = (toRepair.itemMeta as? Damageable)?.takeUnless { it.damage == 0 } ?: return

        toRepairMeta.damage = repairMechanic.finalDamage(toRepair.type.maxDurability.toInt(), toRepairMeta.damage)

        toRepair.setItemMeta(toRepairMeta)
        isCancelled = true
        cursor.amount -= 1
    }
}
