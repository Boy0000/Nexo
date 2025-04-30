package com.nexomc.nexo.utils.wrappers

import org.bukkit.NamespacedKey
import org.bukkit.Registry

object ParticleWrapper {
    @JvmField
    val DUST = Registry.PARTICLE_TYPE[NamespacedKey.minecraft("dust")]!!
    val SPLASH = Registry.PARTICLE_TYPE[NamespacedKey.minecraft("splash")]!!
    val HAPPY_VILLAGER = Registry.PARTICLE_TYPE[NamespacedKey.minecraft("happy_villager")]!!
}