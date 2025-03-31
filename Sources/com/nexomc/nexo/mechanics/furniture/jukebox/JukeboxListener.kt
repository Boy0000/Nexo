package com.nexomc.nexo.mechanics.furniture.jukebox

import com.jeff_media.morepersistentdatatypes.DataType
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.api.events.furniture.NexoFurnitureBreakEvent
import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent
import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.utils.AdventureUtils
import com.nexomc.nexo.utils.AdventureUtils.tagResolver
import com.nexomc.nexo.utils.ItemUtils.isMusicDisc
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.serialize
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.GameMode
import org.bukkit.SoundCategory
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

class JukeboxListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun NexoFurnitureInteractEvent.onInsertDisc() {
        val itemStack = player.inventory.itemInMainHand
        if (hand != EquipmentSlot.HAND || !insertAndPlayDisc(baseEntity, itemStack, player)) return

        player.swingMainHand()

        when {
            itemStack.itemMeta?.hasLore() == true -> itemStack.itemMeta.lore()?.firstOrNull()
            NexoItems.exists(itemStack) && itemStack.itemMeta?.hasDisplayName() == true -> itemStack.itemMeta.displayName()
            else -> null
        }?.let {
            val message = AdventureUtils.MINI_MESSAGE.deserialize(
                Message.MECHANICS_JUKEBOX_NOW_PLAYING.toString(),
                tagResolver("disc", it.serialize())
            )
            player.sendActionBar(message)
        }

        isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun NexoFurnitureInteractEvent.onEjectDisc() {
        if (!ejectAndStopDisc(baseEntity, player)) return
        player.swingMainHand()
        isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun NexoFurnitureBreakEvent.onJukeboxBreak() {
        ejectAndStopDisc(baseEntity, player)
    }

    private fun insertAndPlayDisc(baseEntity: Entity, disc: ItemStack, player: Player?): Boolean {
        val loc = baseEntity.location.toCenterLocation()
        val pdc = baseEntity.persistentDataContainer
        val insertedDisc = disc.clone()
        val jukebox = NexoFurniture.furnitureMechanic(baseEntity)?.jukebox?.takeIf { it.hasPermission(player) } ?: return false
        if (pdc.has(JukeboxBlock.MUSIC_DISC_KEY, DataType.ITEM_STACK) || !isMusicDisc(disc)) return false

        insertedDisc.amount = 1
        if (player != null && player.gameMode != GameMode.CREATIVE) disc.amount -= insertedDisc.amount
        pdc.set(JukeboxBlock.MUSIC_DISC_KEY, DataType.ITEM_STACK, insertedDisc)
        baseEntity.world.playSound(loc, jukebox.playingSong(baseEntity)!!, SoundCategory.RECORDS, jukebox.volume, jukebox.pitch)
        return true
    }

    private fun ejectAndStopDisc(baseEntity: Entity, player: Player?): Boolean {
        val pdc = baseEntity.persistentDataContainer
        val item = pdc.get(JukeboxBlock.MUSIC_DISC_KEY, DataType.ITEM_STACK) ?: return false
        val furnitureMechanic = NexoFurniture.furnitureMechanic(baseEntity) ?: return false
        val jukebox = furnitureMechanic.jukebox?.takeIf { it.hasPermission(player) } ?: return false
        val loc = baseEntity.location.toCenterLocation()
        val songKey = songFromDisc(item)

        if (!pdc.has(JukeboxBlock.MUSIC_DISC_KEY, DataType.ITEM_STACK) || !isMusicDisc(item)) return false

        if (songKey != null) baseEntity.trackedBy.forEach { p ->
            p.stopSound(Sound.sound(songKey, Sound.Source.RECORD, jukebox.volume, jukebox.pitch))
        }
        baseEntity.world.dropItemNaturally(loc, item)
        pdc.remove(JukeboxBlock.MUSIC_DISC_KEY)
        return true
    }

    private fun songFromDisc(disc: ItemStack): Key? {
        return when {
            VersionUtil.atleast("1.21") -> when {
                disc.hasItemMeta() && disc.itemMeta.hasJukeboxPlayable() -> disc.itemMeta.jukeboxPlayable.songKey.key()
                else -> null
            }
            else -> Key.key("minecraft", "music_disc.${disc.type.name.lowercase().substringAfter("music_disc_")}")
        }
    }
}
