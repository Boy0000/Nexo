package com.nexomc.nexo.utils.wrappers

import org.bukkit.NamespacedKey
import org.bukkit.Registry

@Suppress("DEPRECATION")
object EnchantmentWrapper {
    @JvmField
    val FORTUNE = Registry.ENCHANTMENT[NamespacedKey.minecraft("fortune")]!!
    @JvmField
    val EFFICIENCY = Registry.ENCHANTMENT[NamespacedKey.minecraft("efficiency")]!!
    @JvmField
    val SILK_TOUCH = Registry.ENCHANTMENT[NamespacedKey.minecraft("silk_touch")]!!
    @JvmField
    val AQUA_AFFINITY = Registry.ENCHANTMENT[NamespacedKey.minecraft("aqua_affinity")]!!
}
