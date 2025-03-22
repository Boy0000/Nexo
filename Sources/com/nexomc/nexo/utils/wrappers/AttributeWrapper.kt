package com.nexomc.nexo.utils.wrappers

import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.prependIfMissing
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.attribute.Attribute

object AttributeWrapper {
    @JvmField
    val SAFE_FALL_DISTANCE: Attribute? = if (VersionUtil.atleast("1.21.2")) Attribute.SAFE_FALL_DISTANCE else Registry.ATTRIBUTE[NamespacedKey.minecraft("generic.safe_fall_distance")]
    @JvmField
    val MAX_HEALTH: Attribute = if (VersionUtil.atleast("1.21.2")) Attribute.MAX_HEALTH else Registry.ATTRIBUTE[NamespacedKey.minecraft("generic.max_health")]!!
    @JvmField
    val BLOCK_BREAK_SPEED: Attribute? = if (VersionUtil.atleast("1.21.2")) Attribute.BLOCK_BREAK_SPEED else Registry.ATTRIBUTE.get(NamespacedKey.minecraft("player.block_break_speed"))
    @JvmField
    val MINING_EFFICIENCY: Attribute? = if (VersionUtil.atleast("1.21.2")) Attribute.MINING_EFFICIENCY else Registry.ATTRIBUTE.get(NamespacedKey.minecraft("player.mining_efficiency"))
    @JvmField
    val SUBMERGED_MINING_SPEED: Attribute? = if (VersionUtil.atleast("1.21.2")) Attribute.SUBMERGED_MINING_SPEED else Registry.ATTRIBUTE.get(NamespacedKey.minecraft("player.submerged_mining_speed"))
    @JvmField
    val ENTITY_INTERACTION_RANGE: Attribute? = if (VersionUtil.atleast("1.21.2")) Attribute.ENTITY_INTERACTION_RANGE else Registry.ATTRIBUTE[NamespacedKey.minecraft("player.entity_interact_range")]
    @JvmField
    val BLOCK_INTERACTION_RANGE: Attribute? = if (VersionUtil.atleast("1.21.2")) Attribute.BLOCK_INTERACTION_RANGE else Registry.ATTRIBUTE[NamespacedKey.minecraft("player.block_interact_range")]

    @JvmStatic
    fun fromString(attribute: String) = runCatching {
        if (VersionUtil.atleast("1.21.2")) {
            Registry.ATTRIBUTE[NamespacedKey.minecraft(attribute.lowercase().removePrefix("player_").removePrefix("generic_"))]
        } else {
            val key = attribute.lowercase().replace("player_", "player.").replace("generic_", "generic.")
            Registry.ATTRIBUTE[NamespacedKey.minecraft(key)]
                ?: Registry.ATTRIBUTE[NamespacedKey.minecraft(key.prependIfMissing("generic."))]
                ?: Registry.ATTRIBUTE[NamespacedKey.minecraft(key.prependIfMissing("player."))]
        }
    }.getOrNull()
}
