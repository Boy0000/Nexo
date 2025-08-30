package com.nexomc.nexo.mechanics.misc.armor_effects

import com.nexomc.nexo.utils.SchedulerUtils
import kotlinx.coroutines.Job
import org.bukkit.Bukkit

object ArmorEffectsTask {

    fun launchJob(delay: Int): Job {
        return SchedulerUtils.launchDelayed(delay.toLong()) {
            Bukkit.getOnlinePlayers().forEach(ArmorEffectsMechanic::addEffects)
        }
    }
}
