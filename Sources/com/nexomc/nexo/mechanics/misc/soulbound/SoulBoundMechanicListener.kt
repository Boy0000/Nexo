package com.nexomc.nexo.mechanics.misc.soulbound

import com.nexomc.nexo.api.NexoItems
import com.jeff_media.morepersistentdatatypes.DataType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ThreadLocalRandom

class SoulBoundMechanicListener(private val factory: SoulBoundMechanicFactory) : Listener {
    @EventHandler
    fun PlayerDeathEvent.onPlayerDeath() {
        if (keepInventory) return

        val random = ThreadLocalRandom.current()
        val items = mutableListOf<ItemStack>()
        drops.forEach { drop ->
            val itemID = NexoItems.idFromItem(drop)
            if (itemID == null || factory.isNotImplementedIn(itemID)) return@forEach

            val mechanic = factory.getMechanic(itemID)!!
            if (random.nextInt(100) >= mechanic.loseChance * 100) items.add(drop)
        }

        if (items.isNotEmpty()) {
            val player = entity
            val pdc = player.persistentDataContainer
            pdc.set(
                SoulBoundMechanic.NAMESPACED_KEY,
                DataType.ITEM_STACK_ARRAY,
                items.toTypedArray())
            drops.removeAll(items)
        }
    }

    @EventHandler
    fun PlayerRespawnEvent.onPlayerRespawn() {
        val pdc = player.persistentDataContainer
        if (!pdc.has(SoulBoundMechanic.NAMESPACED_KEY, DataType.ITEM_STACK_ARRAY)) return

        val items = pdc.getOrDefault(
            SoulBoundMechanic.NAMESPACED_KEY,
            DataType.ITEM_STACK_ARRAY,
            emptyArray()
        )
        val remainingItems = player.inventory.addItem(*items).values
        for (item in remainingItems) player.world.dropItem(player.location, item)

        pdc.remove(SoulBoundMechanic.NAMESPACED_KEY)
    }
}
