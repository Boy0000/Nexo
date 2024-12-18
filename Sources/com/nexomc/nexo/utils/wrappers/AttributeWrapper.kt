package com.nexomc.nexo.utils.wrappers

import com.nexomc.nexo.utils.VersionUtil
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.attribute.Attribute

object AttributeWrapper {
    @JvmField
    val MAX_HEALTH = if (VersionUtil.atleast("1.21.2")) Attribute.MAX_HEALTH else Registry.ATTRIBUTE[NamespacedKey.minecraft("generic.max_health")]!!
    @JvmField
    val BLOCK_BREAK_SPEED = if (VersionUtil.atleast("1.21.2")) Attribute.BLOCK_BREAK_SPEED else Registry.ATTRIBUTE[NamespacedKey.minecraft("player.block_break_speed")]

    @JvmStatic
    fun fromString(attribute: String) = runCatching {
        Registry.ATTRIBUTE[NamespacedKey.minecraft(attribute.lowercase().replace("player_", "player.").replace("generic_", "generic."))]
    }.getOrNull()
}
