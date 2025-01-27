package com.nexomc.nexo.mechanics.furniture.listeners

import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager
import io.papermc.paper.event.entity.EntityMoveEvent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockFormEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerMoveEvent
import java.util.*

class FurnitureBarrierHitboxListener : Listener {

    @EventHandler(ignoreCancelled = true)
    fun BlockFromToEvent.onFlowThroughBarrier() {
        val toLoc = toBlock.location
        if (IFurniturePacketManager.barrierHitboxLocationMap.any { toLoc in it.value }) isCancelled = true
    }

    @EventHandler
    fun BlockFormEvent.onSnow() {
        val loc = block.location
        if (IFurniturePacketManager.barrierHitboxLocationMap.any { loc in it.value }) isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun EntityMoveEvent.onMobMove() {
        val toLoc = to.block.location
        if (hasExplicitlyChangedBlock() && IFurniturePacketManager.barrierHitboxLocationMap.any { toLoc in it.value }) isCancelled = true
    }

    private val flightCache: MutableSet<UUID> = mutableSetOf()
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    fun PlayerMoveEvent.onMove() {
        if (from.blockX != to.blockX || from.blockY != to.blockY || from.blockZ != to.blockZ)
            flightCache -= player.uniqueId
    }

    @EventHandler(ignoreCancelled = true)
    fun PlayerKickEvent.onKick() {
        if (Bukkit.getServer().allowFlight || cause != PlayerKickEvent.Cause.FLYING_PLAYER) return
        if (player.uniqueId !in flightCache && !IFurniturePacketManager.standingOnFurniture(player)) return
        flightCache += player.uniqueId
        isCancelled = true
    }
}