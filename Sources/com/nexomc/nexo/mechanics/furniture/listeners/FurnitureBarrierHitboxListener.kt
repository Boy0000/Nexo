package com.nexomc.nexo.mechanics.furniture.listeners

import com.nexomc.nexo.api.events.furniture.NexoFurnitureBreakEvent
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager
import io.papermc.paper.event.entity.EntityMoveEvent
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.block.BlockFace
import org.bukkit.entity.ArmorStand
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockFormEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.entity.EntityPlaceEvent
import org.bukkit.event.hanging.HangingBreakEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerMoveEvent

class FurnitureBarrierHitboxListener : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun HangingBreakEvent.onHangingBreak() {
        if (cause != HangingBreakEvent.RemoveCause.PHYSICS) return
        val relative = entity.location.block.getRelative(entity.attachedFace)
        if (IFurniturePacketManager.blockIsHitbox(relative)) isCancelled = true
    }

    @EventHandler
    fun EntityPlaceEvent.onPlace() {
        if (entity !is ArmorStand || !IFurniturePacketManager.blockIsHitbox(block)) return
        entity.setGravity(false)
    }

    @EventHandler
    fun NexoFurnitureBreakEvent.onBreak() {
        val hitboxLocs = IFurniturePacketManager.barrierHitboxLocationMap.get(baseEntity.uniqueId) ?: return
        baseEntity.world.getNearbyEntitiesByType(ArmorStand::class.java, baseEntity.location, 8.0).forEach {
            if (it.location.block.getRelative(BlockFace.DOWN).location in hitboxLocs) it.setGravity(true)
        }
    }

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
        val (to, from) = to.block.location to from.block.location
        if (!hasExplicitlyChangedBlock() || to.y < from.y) return
        if (IFurniturePacketManager.blockIsHitbox(to.block) || IFurniturePacketManager.blockIsHitbox(from.block)) isCancelled = true
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