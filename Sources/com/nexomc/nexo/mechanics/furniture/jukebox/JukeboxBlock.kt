package com.nexomc.nexo.mechanics.furniture.jukebox

import com.jeff_media.morepersistentdatatypes.DataType
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.utils.VersionUtil
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

class JukeboxBlock(val permission: String = "", val volume: Float = 1.0f, val pitch: Float = 1.0f) {

    constructor(section: ConfigurationSection) : this(
        section.getString("permission") ?: "",
        section.getDouble("volume", 1.0).toFloat(),
        section.getDouble("pitch", 1.0).toFloat()
    )

    fun playingSong(baseEntity: Entity): String? {
        val disc = baseEntity.persistentDataContainer.get(MUSIC_DISC_KEY, DataType.ITEM_STACK) ?: return null
        return when {
            VersionUtil.below("1.20.5") -> when {
                disc.type.isRecord() -> disc.type.name.lowercase().replace("music_disc_", "minecraft:music_disc.")
                else -> null
            }
            else -> when {
                disc.hasItemMeta() && disc.itemMeta.hasJukeboxPlayable() -> disc.itemMeta.jukeboxPlayable.songKey.asString()
                else -> null
            }
        }
    }

    fun hasPermission(player: Player?) = player == null || permission.isBlank() || player.hasPermission(permission)

    companion object {
        val MUSIC_DISC_KEY = NamespacedKey(NexoPlugin.instance(), "music_disc")
    }
}
