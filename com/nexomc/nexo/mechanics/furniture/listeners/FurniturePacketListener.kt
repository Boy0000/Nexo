package com.nexomc.nexo.mechanics.furniture.listeners

import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent
import com.mineinabyss.idofront.operators.plus
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.api.events.NexoMechanicsRegisteredEvent
import com.nexomc.nexo.api.events.furniture.NexoFurnitureBreakEvent
import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent
import com.nexomc.nexo.mechanics.custom_block.CustomBlockHelpers
import com.nexomc.nexo.mechanics.furniture.FurnitureFactory
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager
import com.nexomc.nexo.mechanics.furniture.seats.FurnitureSeat
import com.nexomc.nexo.utils.EventUtils.call
import com.nexomc.nexo.utils.logs.Logs
import io.papermc.paper.event.player.PlayerFailMoveEvent
import io.papermc.paper.event.player.PlayerTrackEntityEvent
import io.papermc.paper.event.player.PlayerUntrackEntityEvent
import io.th0rgal.protectionlib.ProtectionLib
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.ItemDisplay
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.util.Vector
import java.util.UUID

class FurniturePacketListener : Listener {
    @EventHandler
    fun PlayerTrackEntityEvent.onPlayerTrackFurniture() {
        val itemDisplay = entity as? ItemDisplay ?: return
        val mechanic = NexoFurniture.furnitureMechanic(itemDisplay) ?: return
        val packetManager = FurnitureFactory.instance()?.packetManager() ?: return

        Bukkit.getScheduler().runTaskLater(NexoPlugin.instance(), Runnable {
            packetManager.sendFurnitureEntityPacket(itemDisplay, mechanic, player)
            packetManager.sendInteractionEntityPacket(itemDisplay, mechanic, player)
            packetManager.sendBarrierHitboxPacket(itemDisplay, mechanic, player)
            packetManager.sendLightMechanicPacket(itemDisplay, mechanic, player)
        }, 2L)
    }

    @EventHandler
    fun PlayerUntrackEntityEvent.onPlayerUntrackFurniture() {
        val itemDisplay = entity as? ItemDisplay ?: return
        val mechanic = NexoFurniture.furnitureMechanic(itemDisplay) ?: return
        val packetManager = FurnitureFactory.instance()?.packetManager() ?: return

        packetManager.removeFurnitureEntityPacket(itemDisplay, mechanic, player)
        packetManager.removeInteractionHitboxPacket(itemDisplay, mechanic, player)
        packetManager.removeBarrierHitboxPacket(itemDisplay, mechanic, player)
        packetManager.removeLightMechanicPacket(itemDisplay, mechanic, player)
    }

    @EventHandler
    fun NexoMechanicsRegisteredEvent.onFurnitureFactory() {
        val packetManager = FurnitureFactory.instance()?.packetManager() ?: return
        Bukkit.getWorlds().flatMap { it.entities }.filterIsInstance<ItemDisplay>().forEach { baseEntity ->
            val mechanic = NexoFurniture.furnitureMechanic(baseEntity) ?: return@forEach
            if (FurnitureSeat.isSeat(baseEntity)) return@forEach

            packetManager.sendFurnitureEntityPacket(baseEntity, mechanic)
            packetManager.sendInteractionEntityPacket(baseEntity, mechanic)
            packetManager.sendBarrierHitboxPacket(baseEntity, mechanic)
            packetManager.sendLightMechanicPacket(baseEntity, mechanic)
        }
    }

    @EventHandler
    fun PlayerUseUnknownEntityEvent.onUseUnknownEntity() {
        val itemStack = when (hand) {
            EquipmentSlot.HAND -> player.inventory.itemInMainHand
            else -> player.inventory.itemInOffHand
        }
        val interactionPoint = clickedRelativePosition?.toLocation(player.world)
        val baseEntity = IFurniturePacketManager.baseEntityFromHitbox(entityId) ?: return
        val mechanic = NexoFurniture.furnitureMechanic(baseEntity) ?: return
        if (FurnitureSeat.isSeat(baseEntity)) return

        when {
            ProtectionLib.canBreak(player, baseEntity.location) && isAttack -> {
                NexoFurnitureBreakEvent(mechanic, baseEntity, player).call {
                    NexoFurniture.remove(baseEntity, player)
                }
            }

            ProtectionLib.canInteract(player, baseEntity.location) ->
                NexoFurnitureInteractEvent(mechanic, baseEntity, player, itemStack, hand, interactionPoint).call()
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun PlayerInteractEvent.onPlayerInteractBarrierHitbox() {
        val mechanic = NexoFurniture.furnitureMechanic(clickedBlock) ?: NexoFurniture.furnitureMechanic(interactionPoint) ?: return
        val baseEntity = mechanic.baseEntity(clickedBlock) ?: mechanic.baseEntity(interactionPoint) ?: return

        when {
            action == Action.RIGHT_CLICK_BLOCK && ProtectionLib.canInteract(player, baseEntity.location) -> {

                val validBlockItem = item != null && !NexoFurniture.isFurniture(item) && item!!.type.isBlock
                if (useItemInHand() != Event.Result.DENY && validBlockItem && (!mechanic.isInteractable || player.isSneaking)) {
                    setUseItemInHand(Event.Result.DENY)
                    clickedBlock?.type = Material.BARRIER
                    CustomBlockHelpers.makePlayerPlaceBlock(player, hand!!, item!!, clickedBlock!!, blockFace, null, null)
                    clickedBlock?.type = Material.AIR
                }

                NexoFurnitureInteractEvent(mechanic, baseEntity, player, item, hand!!, interactionPoint, useInteractedBlock(), useItemInHand(), blockFace).call {
                    setUseInteractedBlock(useFurniture)
                    setUseItemInHand(useItemInHand)
                }
            }
            action == Action.LEFT_CLICK_BLOCK && ProtectionLib.canBreak(player, baseEntity.location) -> {
                NexoFurnitureBreakEvent(mechanic, baseEntity, player).call {
                    NexoFurniture.remove(baseEntity, player)
                }
            }
        }

        // Resend the hitbox as client removes the "ghost block"
        Bukkit.getScheduler().scheduleSyncDelayedTask(
            NexoPlugin.instance(), {
                val packetManager = FurnitureFactory.instance()?.packetManager() ?: return@scheduleSyncDelayedTask
                packetManager.sendBarrierHitboxPacket(baseEntity, mechanic)
                packetManager.sendLightMechanicPacket(baseEntity, mechanic)
            }, 2L
        )
    }

    private val flightCache: MutableSet<UUID> = mutableSetOf()
    @EventHandler
    fun PlayerMoveEvent.onMove() {
        flightCache -= player.uniqueId
    }

    @EventHandler(ignoreCancelled = true)
    fun PlayerKickEvent.onKick() {
        if (cause != PlayerKickEvent.Cause.FLYING_PLAYER && player.uniqueId !in flightCache && !IFurniturePacketManager.standingOnFurniture(player)) return
        flightCache += player.uniqueId
        isCancelled = true
    }
}
