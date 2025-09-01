package com.nexomc.nexo.mechanics.misc.armor_effects

import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable

class ArmorEffectsTask : BukkitRunnable() {
    override fun run() {
        Bukkit.getOnlinePlayers().forEach(ArmorEffectsMechanic::addEffects)
    }
}
