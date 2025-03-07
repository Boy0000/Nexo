package com.nexomc.nexo.utils.wrappers

import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.potion.PotionEffectType

@Suppress("DEPRECATION")
object PotionEffectTypeWrapper {
    @JvmField
    val HASTE = getEffectType("haste")!!
    @JvmField
    val CONDUIT_POWER = getEffectType("conduit_power")!!
    @JvmField
    val MINING_FATIGUE = getEffectType("mining_fatigue")!!
    @JvmField
    val WIND_CHARGED = getEffectType("wind_charged")

    private fun getEffectType(effect: String?): PotionEffectType? {
        if (effect.isNullOrEmpty()) return null
        return runCatching { Registry.POTION_EFFECT_TYPE[NamespacedKey.minecraft(effect)] }.getOrNull()
            ?: PotionEffectType.getByName(effect)
            ?: PotionEffectType.getByKey(NamespacedKey.minecraft(effect))
    }
}
