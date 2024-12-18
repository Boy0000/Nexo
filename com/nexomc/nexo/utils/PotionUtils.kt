package com.nexomc.nexo.utils

import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.potion.PotionEffectType

@Suppress("DEPRECATION")
object PotionUtils {
    fun getEffectType(effect: String?): PotionEffectType? {
        if (effect.isNullOrEmpty()) return null
        return runCatching { Registry.POTION_EFFECT_TYPE.get(NamespacedKey.fromString(effect)!!) }.getOrNull()
            ?: PotionEffectType.getByName(effect)
            ?: PotionEffectType.getByKey(NamespacedKey.fromString(effect))
    }
}
