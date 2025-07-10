package com.nexomc.nexo.mechanics.furniture.listeners

import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.breaker.FurnitureBreakerManager
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageAbortEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent

class FurnitureBreakListener : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun BlockDamageEvent.onDamageFurniture() {
        val baseEntity = NexoFurniture.baseEntity(block) ?: return
        val mechanic = NexoFurniture.furnitureMechanic(baseEntity)?.takeUnless { it.breakable.hardness == 0.0 } ?: return
        if (VersionUtil.below("1.20.5")) isCancelled = true
        if (player.gameMode == GameMode.CREATIVE) return

        FurnitureBreakerManager.startFurnitureBreak(player, baseEntity, mechanic, block)
    }

    @EventHandler
    fun BlockDamageAbortEvent.onAbort() {
        FurnitureBreakerManager.stopFurnitureBreak(player)
    }

    @EventHandler
    fun BlockBreakEvent.onBlockBreak() {
        FurnitureBreakerManager.stopFurnitureBreak(player)
    }

    @EventHandler
    fun PlayerQuitEvent.onDisconnect() {
        FurnitureBreakerManager.stopFurnitureBreak(player)
    }

    @EventHandler
    fun PlayerSwapHandItemsEvent.onSwapHand() {
        FurnitureBreakerManager.stopFurnitureBreak(player)
    }

    @EventHandler
    fun PlayerDropItemEvent.onDropHand() {
        FurnitureBreakerManager.stopFurnitureBreak(player)
    }
}