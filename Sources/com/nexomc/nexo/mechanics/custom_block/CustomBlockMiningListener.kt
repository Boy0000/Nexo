package com.nexomc.nexo.mechanics.custom_block

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.utils.VersionUtil
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

class CustomBlockMiningListener : Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun BlockDamageEvent.onDamageCustomBlock() {
        val mechanic = NexoBlocks.customBlockMechanic(block) ?: return
        if (player.gameMode == GameMode.CREATIVE) return

        if (VersionUtil.below("1.20.5")) isCancelled = true
        NexoPlugin.instance().breakerManager().startBlockBreak(player, block, mechanic)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun BlockDamageAbortEvent.onDamageAbort() {
        NexoPlugin.instance().breakerManager().stopBlockBreak(player)
    }

    @EventHandler
    fun BlockBreakEvent.onBlockBreak() {
        if (VersionUtil.atleast("1.20.5")) NexoPlugin.instance().breakerManager().stopBlockBreak(player)
    }

    @EventHandler
    fun PlayerQuitEvent.onDisconnect() {
        NexoPlugin.instance().breakerManager().stopBlockBreak(player)
    }

    @EventHandler
    fun PlayerSwapHandItemsEvent.onSwapHand() {
        NexoPlugin.instance().breakerManager().stopBlockBreak(player)
    }

    @EventHandler
    fun PlayerDropItemEvent.onDropHand() {
        NexoPlugin.instance().breakerManager().stopBlockBreak(player)
    }
}
