package com.nexomc.nexo.mechanics.furniture.listeners

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent
import com.jeff_media.morepersistentdatatypes.DataType
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.api.events.NexoMechanicsRegisteredEvent
import com.nexomc.nexo.api.events.furniture.NexoFurnitureBreakEvent
import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent
import com.nexomc.nexo.mechanics.custom_block.CustomBlockHelpers
import com.nexomc.nexo.mechanics.furniture.FurnitureFactory
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager.Companion.furnitureBaseMap
import com.nexomc.nexo.mechanics.furniture.seats.FurnitureSeat
import com.nexomc.nexo.utils.EventUtils.call
import com.nexomc.nexo.utils.SchedulerUtils
import com.nexomc.nexo.utils.filterFastIsInstance
import com.nexomc.nexo.utils.flatMapFast
import io.papermc.paper.event.player.PlayerTrackEntityEvent
import io.papermc.paper.event.player.PlayerUntrackEntityEvent
import io.th0rgal.protectionlib.ProtectionLib
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDismountEvent
import org.bukkit.event.entity.EntityMountEvent
import org.bukkit.event.entity.EntityTeleportEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import org.bukkit.event.vehicle.VehicleExitEvent
import org.bukkit.inventory.EquipmentSlot
import java.util.*

class FurniturePacketListener : Listener {
    @EventHandler
    fun PlayerTrackEntityEvent.onPlayerTrackFurniture() {
        val itemDisplay = entity as? ItemDisplay ?: return
        val mechanic = NexoFurniture.furnitureMechanic(itemDisplay) ?: return
        val packetManager = FurnitureFactory.instance()?.packetManager() ?: return

        SchedulerUtils.runTaskLater(2L) {
            packetManager.sendFurnitureMetadataPacket(itemDisplay, mechanic, player)
            packetManager.sendInteractionEntityPacket(itemDisplay, mechanic, player)
            packetManager.sendBarrierHitboxPacket(itemDisplay, mechanic, player)
            packetManager.sendLightMechanicPacket(itemDisplay, mechanic, player)
        }
    }

    @EventHandler
    fun PlayerUntrackEntityEvent.onPlayerUntrackFurniture() {
        val itemDisplay = entity as? ItemDisplay ?: return
        val mechanic = NexoFurniture.furnitureMechanic(itemDisplay) ?: return
        val packetManager = FurnitureFactory.instance()?.packetManager() ?: return

        packetManager.removeInteractionHitboxPacket(itemDisplay, mechanic, player)
        packetManager.removeBarrierHitboxPacket(itemDisplay, mechanic, player)
        packetManager.removeLightMechanicPacket(itemDisplay, mechanic, player)
    }

    @EventHandler
    fun EntityAddToWorldEvent.onLoad() {
        val itemDisplay = entity as? ItemDisplay ?: return
        furnitureBaseMap.removeIf { it.baseUuid == itemDisplay.uniqueId && it.baseId != itemDisplay.entityId }
    }

    @EventHandler
    fun EntityRemoveFromWorldEvent.onUnload() {
        val itemDisplay = (entity as? ItemDisplay)?.takeIf { it.location.isChunkLoaded } ?: return
        val mechanic = NexoFurniture.furnitureMechanic(itemDisplay) ?: return
        val packetManager = FurnitureFactory.instance()?.packetManager() ?: return

        furnitureBaseMap.removeIf { it.baseUuid == itemDisplay.uniqueId }
        packetManager.removeInteractionHitboxPacket(itemDisplay, mechanic)
        packetManager.removeBarrierHitboxPacket(itemDisplay, mechanic)
        packetManager.removeLightMechanicPacket(itemDisplay, mechanic)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun EntityTeleportEvent.onTeleportFurniture() {
        val baseEntity = entity as? ItemDisplay ?: return
        val mechanic = NexoFurniture.furnitureMechanic(baseEntity) ?: return

        mechanic.hitbox.refreshHitboxes(baseEntity, mechanic)
    }

    @EventHandler
    fun NexoMechanicsRegisteredEvent.onFurnitureFactory() {
        val packetManager = FurnitureFactory.instance()?.packetManager() ?: return
        Bukkit.getWorlds().flatMapFast { it.entities }.filterFastIsInstance<ItemDisplay>().forEach { baseEntity ->
            val mechanic = NexoFurniture.furnitureMechanic(baseEntity) ?: return@forEach
            if (FurnitureSeat.isSeat(baseEntity)) return@forEach

            packetManager.sendFurnitureMetadataPacket(baseEntity, mechanic)
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
            isAttack && player.gameMode != GameMode.ADVENTURE && ProtectionLib.canBreak(player, baseEntity.location) -> {
                NexoFurnitureBreakEvent(mechanic, baseEntity, player).call {
                    NexoFurniture.remove(baseEntity, player)
                }
            }

            ProtectionLib.canInteract(player, baseEntity.location) && clickedRelativePosition != null ->
                NexoFurnitureInteractEvent(mechanic, baseEntity, player, itemStack, hand, interactionPoint).call()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun PlayerInteractEvent.onPlayerInteractBarrierHitbox() {
        if (!IFurniturePacketManager.blockIsHitbox(clickedBlock ?: interactionPoint?.block ?: return)) return
        val mechanic = NexoFurniture.furnitureMechanic(clickedBlock) ?: NexoFurniture.furnitureMechanic(interactionPoint) ?: return
        val baseEntity = FurnitureMechanic.baseEntity(clickedBlock) ?: FurnitureMechanic.baseEntity(interactionPoint) ?: return

        if (useInteractedBlock() != Event.Result.DENY) when {
            action == Action.RIGHT_CLICK_BLOCK && ProtectionLib.canInteract(player, baseEntity.location) -> {
                val validBlockItem = item != null && !NexoFurniture.isFurniture(item) && item!!.type.let { it.isBlock && it != Material.LILY_PAD && it != Material.FROGSPAWN }
                if (useItemInHand() != Event.Result.DENY && validBlockItem && (!mechanic.isInteractable || player.isSneaking) && ProtectionLib.canBuild(player, baseEntity.location)) {
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
        SchedulerUtils.syncDelayedTask(2L) {
            FurnitureFactory.instance()?.packetManager()?.apply {
                sendBarrierHitboxPacket(baseEntity, mechanic)
                sendLightMechanicPacket(baseEntity, mechanic)
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    fun EntityMountEvent.onSitSeat() {
        val player = entity as? Player ?: return
        val baseEntity = mount.persistentDataContainer.get(FurnitureSeat.SEAT_KEY, DataType.UUID)?.let(Bukkit::getEntity) as? ItemDisplay ?: return
        val mechanic = NexoFurniture.furnitureMechanic(baseEntity) ?: return

        SchedulerUtils.runTaskLater(4L) {
            FurnitureFactory.instance()?.packetManager()?.removeBarrierHitboxPacket(baseEntity, mechanic, player)
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    fun EntityDismountEvent.onLeaveSeat() {
        val player = entity as? Player ?: return
        val baseEntity = dismounted.persistentDataContainer.get(FurnitureSeat.SEAT_KEY, DataType.UUID)?.let(Bukkit::getEntity) as? ItemDisplay ?: return
        val mechanic = NexoFurniture.furnitureMechanic(baseEntity) ?: return

        FurnitureFactory.instance()?.packetManager()?.sendBarrierHitboxPacket(baseEntity, mechanic, player)
    }
}
