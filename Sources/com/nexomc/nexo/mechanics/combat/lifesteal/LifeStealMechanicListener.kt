package com.nexomc.nexo.mechanics.combat.lifesteal

import com.nexomc.nexo.utils.ItemUtils
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.wrappers.AttributeWrapper
import com.nexomc.protectionlib.ProtectionLib
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent

class LifeStealMechanicListener(private val factory: LifeStealMechanicFactory) : Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun EntityDamageByEntityEvent.onCall() {
        val damager = damager as? Player ?: return
        val item = damager.inventory.itemInMainHand.takeUnless { VersionUtil.atleast("1.21.2") && damager.hasCooldown(it) } ?: return
        val livingEntity = entity as? LivingEntity ?: return
        if (!ProtectionLib.canInteract(damager, entity.location)) return

        val mechanic = factory.getMechanic(item) ?: return
        val maxHealth = damager.getAttribute(AttributeWrapper.MAX_HEALTH)!!.value
        damager.health = (damager.health + mechanic.amount).coerceAtMost(maxHealth)
        livingEntity.health = (livingEntity.health - mechanic.amount).coerceAtLeast(0.0)

        ItemUtils.triggerCooldown(damager, item)
    }
}
