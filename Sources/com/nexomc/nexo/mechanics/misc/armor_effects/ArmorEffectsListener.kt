package com.nexomc.nexo.mechanics.misc.armor_effects

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class ArmorEffectsListener : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun PlayerArmorChangeEvent.onItemEquipped() {
        ArmorEffectsMechanic.removeEffects(player, oldItem)
        ArmorEffectsMechanic.addEffects(player)
    }
}