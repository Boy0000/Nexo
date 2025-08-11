package com.nexomc.nexo.mechanics.furniture.listeners

import com.nexomc.nexo.api.events.furniture.NexoFurnitureBreakEvent
import com.nexomc.nexo.mechanics.furniture.FurnitureFactory
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager
import io.papermc.paper.event.entity.EntityMoveEvent
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.bukkit.Bukkit
import org.bukkit.block.BlockFace
import org.bukkit.entity.ArmorStand
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockFormEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockGrowEvent
import org.bukkit.event.entity.EntityPlaceEvent
import org.bukkit.event.hanging.HangingBreakEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.world.StructureGrowEvent
import java.util.*

class FurnitureBarrierHitboxListener() : Listener {

    init {
        if (FurnitureFactory.instance()?.handleNonPlayerBarrierCollision == true) {
            FurnitureFactory.instance()?.registerListeners(object : Listener {
                @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
                fun EntityMoveEvent.onMobMove() {
                    if (!hasExplicitlyChangedBlock() || to.y < from.y) return
                    if (IFurniturePacketManager.blockIsHitbox(to) || IFurniturePacketManager.blockIsHitbox(from)) isCancelled = true
                }
            })
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun HangingBreakEvent.onHangingBreak() {
        if (cause != HangingBreakEvent.RemoveCause.PHYSICS) return
        val relative = entity.location.block.getRelative(entity.attachedFace)
        if (IFurniturePacketManager.blockIsHitbox(relative, collisionOnly = false)) isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun EntityPlaceEvent.onPlace() {
        if (entity !is ArmorStand || !IFurniturePacketManager.blockIsHitbox(block, collisionOnly = false)) return
        entity.setGravity(false)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun NexoFurnitureBreakEvent.onBreak() {
        val hitboxLocs = IFurniturePacketManager.barrierHitboxLocationMap.get(baseEntity.uniqueId) ?: return
        baseEntity.world.getNearbyEntitiesByType(ArmorStand::class.java, baseEntity.location, 8.0).forEach {
            if (it.location.block.getRelative(BlockFace.DOWN).location in hitboxLocs) it.setGravity(true)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun BlockFromToEvent.onFlowThroughBarrier() {
        val toLoc = toBlock.location
        if (IFurniturePacketManager.barrierHitboxLocationMap.any { toLoc in it.value }) isCancelled = true
    }

    @EventHandler
    fun BlockGrowEvent.onGrow() {
        val loc = block.location
        if (IFurniturePacketManager.barrierHitboxLocationMap.any { loc in it.value }) isCancelled = true
    }

    @EventHandler
    fun StructureGrowEvent.onGrow() {
        val locs = blocks.map { it.location }
        if (IFurniturePacketManager.barrierHitboxLocationMap.any { locs.any(it.value::contains) }) isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun BlockFormEvent.onSnow() {
        val loc = block.location
        if (IFurniturePacketManager.barrierHitboxLocationMap.any { loc in it.value }) isCancelled = true
    }

    private val flightCache: ObjectOpenHashSet<UUID> = ObjectOpenHashSet()
    init {
        if (FurnitureFactory.instance()!!.tryPreventingBarrierKick && !Bukkit.getServer().allowFlight) {
            FurnitureFactory.instance()?.registerListeners(object : Listener {
                @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
                fun PlayerMoveEvent.onMove() {
                    if (!hasExplicitlyChangedBlock()) return
                    flightCache -= player.uniqueId
                }

                @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
                fun PlayerKickEvent.onKick() {
                    if (cause != PlayerKickEvent.Cause.FLYING_PLAYER) return
                    if (player.uniqueId !in flightCache && !IFurniturePacketManager.standingOnFurniture(player)) return
                    flightCache += player.uniqueId
                    isCancelled = true
                }
            })
        }
    }
}