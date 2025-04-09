package com.nexomc.nexo.mechanics.furniture.listeners

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import com.mineinabyss.idofront.items.asColorable
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent
import com.nexomc.nexo.api.events.furniture.NexoFurniturePlaceEvent
import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.mechanics.furniture.BlockLocation
import com.nexomc.nexo.mechanics.furniture.FurnitureFactory
import com.nexomc.nexo.mechanics.furniture.FurnitureHelpers
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic.RestrictedRotation
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager
import com.nexomc.nexo.mechanics.furniture.bed.FurnitureBed
import com.nexomc.nexo.mechanics.furniture.seats.FurnitureSeat
import com.nexomc.nexo.mechanics.limitedplacing.LimitedPlacing.LimitedPlacingType
import com.nexomc.nexo.mechanics.storage.StorageType
import com.nexomc.nexo.utils.BlockHelpers
import com.nexomc.nexo.utils.EventUtils.call
import com.nexomc.nexo.utils.SchedulerUtils
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.serialize
import com.nexomc.nexo.utils.to
import com.nexomc.protectionlib.ProtectionLib
import io.papermc.paper.event.player.PlayerPickItemEvent
import org.bukkit.GameEvent
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Rotation
import org.bukkit.block.BlockFace
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryCreativeEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.persistence.PersistentDataType

class FurnitureListener : Listener {
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun PlayerInteractEvent.onLimitedPlacing() {
        val (block, itemId) = (clickedBlock?.takeIf { action == Action.RIGHT_CLICK_BLOCK } ?: return) to (item?.let(NexoItems::idFromItem) ?: return)
        val limitedPlacing = NexoFurniture.furnitureMechanic(itemId)?.limitedPlacing ?: return
        val belowPlaced = block.getRelative(blockFace).getRelative(BlockFace.DOWN)
        if (!player.isSneaking && BlockHelpers.isInteractable(clickedBlock, player)) return

        when {
            limitedPlacing.isNotPlacableOn(block, blockFace) -> isCancelled = true
            limitedPlacing.type == LimitedPlacingType.ALLOW && !limitedPlacing.checkLimited(belowPlaced) -> isCancelled = true
            limitedPlacing.type == LimitedPlacingType.DENY && limitedPlacing.checkLimited(belowPlaced) -> isCancelled = true
            else -> {
                val (radius, amount) = limitedPlacing.radiusLimitation?.let { it.radius.toDouble() to it.amount.toDouble()} ?: return
                if (block.world.getNearbyEntitiesByType(ItemDisplay::class.java, block.location, radius)
                        .filter { NexoFurniture.furnitureMechanic(it)?.itemID == itemId }
                        .count { it.location.distanceSquared(block.location) <= radius * radius } >= amount
                ) isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun PlayerInteractEvent.onFurniturePlace() {
        val block = clickedBlock?.let { if (BlockHelpers.isReplaceable(it)) it else it.getRelative(blockFace) } ?: return
        val (item, hand) = (item ?: return) to (hand?.takeIf { it == EquipmentSlot.HAND } ?: return)
        val mechanic = FurnitureFactory.instance()?.getMechanic(item) ?: return

        if (action != Action.RIGHT_CLICK_BLOCK || (useInteractedBlock() == Event.Result.DENY && !player.isSneaking) || useItemInHand() == Event.Result.DENY) return
        if (!NexoFurniture.isFurniture(item) || BlockHelpers.isStandingInside(player, block)) return
        if (!ProtectionLib.canBuild(player, block.location)) return
        if (mechanic.farmlandRequired && block.getRelative(BlockFace.DOWN).type != Material.FARMLAND) return
        if (!player.isSneaking && BlockHelpers.isInteractable(clickedBlock, player)) return

        val blockPlaceEvent = BlockPlaceEvent(block, block.state, clickedBlock!!, item, player, true, hand)
        val rotation = getRotation(player.eyeLocation.yaw.toDouble(), mechanic)
        val yaw = FurnitureHelpers.correctedYaw(mechanic, FurnitureHelpers.rotationToYaw(rotation))

        if (player.gameMode == GameMode.ADVENTURE) blockPlaceEvent.isCancelled = true
        if (mechanic.notEnoughSpace(block.location, yaw)) {
            blockPlaceEvent.isCancelled = true
            Message.NOT_ENOUGH_SPACE.send(player)
        }

        if (!blockPlaceEvent.canBuild() || !blockPlaceEvent.call()) return

        val baseEntity = mechanic.place(block.location, yaw, blockFace, false) ?: return
        val pdc = baseEntity.persistentDataContainer

        item.itemMeta?.asColorable()?.color?.asRGB()?.also {
            pdc.set(FurnitureMechanic.FURNITURE_DYE_KEY, PersistentDataType.INTEGER, it)
        }
        item.itemMeta.displayName()?.serialize()?.also {
            pdc.set(FurnitureMechanic.DISPLAY_NAME_KEY, PersistentDataType.STRING, it)
        }
        player.swingHand(hand)

        if (!NexoFurniturePlaceEvent(mechanic, block, baseEntity, player, item, hand).call()) {
            NexoFurniture.remove(baseEntity)
            return
        }

        // Override replaceable blocks that aren't water if furniture is waterloggable
        if (!mechanic.waterloggable || block.type != Material.WATER) block.type = Material.AIR
        if (player.gameMode != GameMode.CREATIVE) item.amount -= 1
        setUseInteractedBlock(Event.Result.DENY)
        baseEntity.world.sendGameEvent(player, GameEvent.BLOCK_PLACE, baseEntity.location.toVector())
    }

    @EventHandler
    fun PlayerInteractEvent.onPlaceAgainstFurniture() {
        val (block, blockLoc, itemStack) = (clickedBlock ?: return) to (BlockLocation(clickedBlock!!.location)) to (item ?: return)
        val (isItemFrame, isArmorStand, isPainting) = with(itemStack.type) {
            ("ITEM_FRAME" in name) to ("ARMOR_STAND" in name) to ("PAINTING" in name)
        }

        if (action != Action.RIGHT_CLICK_BLOCK || useItemInHand() == Event.Result.DENY) return
        if (!itemStack.type.isBlock && !isItemFrame && !isArmorStand && !isPainting) return

        val baseEntity = IFurniturePacketManager.baseEntityFromHitbox(blockLoc) ?: return
        NexoFurniture.furnitureMechanic(baseEntity) ?: return

        // Since the server-side block is AIR by default, placing blocks acts weird
        // Temporarily set the block to a barrier, then schedule a task to revert it next tick and resend hitboxes
        block.setType(Material.BARRIER, false)
        SchedulerUtils.foliaScheduler.runAtLocation(block.location) { block.setType(Material.AIR, false) }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun NexoFurnitureInteractEvent.onFurnitureInteract() {
        val allowUseItem = useItemInHand
        useItemInHand = Event.Result.DENY

        if (hand == EquipmentSlot.HAND) {
            if (canRunAction != Event.Result.DENY) mechanic.runClickActions(player)
            if (canToggleLight != Event.Result.DENY) FurnitureHelpers.toggleLight(baseEntity, mechanic = mechanic)

            when {
                canRotate != Event.Result.DENY -> mechanic.rotateFurniture(baseEntity)
                canOpenStorage != Event.Result.DENY -> when (mechanic.storage?.storageType) {
                    StorageType.STORAGE, StorageType.SHULKER -> mechanic.storage.openStorage(baseEntity, player)
                    StorageType.PERSONAL -> mechanic.storage.openPersonalStorage(player, baseEntity.location, baseEntity)
                    StorageType.DISPOSAL -> mechanic.storage.openDisposal(player, baseEntity.location, baseEntity)
                    StorageType.ENDERCHEST -> player.openInventory(player.enderChest)
                    null -> return
                }
                canSit != Event.Result.DENY -> FurnitureSeat.sitOnSeat(baseEntity, player, interactionPoint)
                canSleep != Event.Result.DENY -> FurnitureBed.sleepOnBed(baseEntity, player, interactionPoint)
                canRunAction == Event.Result.DENY && canToggleLight == Event.Result.DENY -> useItemInHand = allowUseItem
            }
        }

        player.updateInventory()
    }

    // Does not work for 1.21.4, packets are intercepted for that atm
    @EventHandler(priority = EventPriority.LOWEST)
    fun PlayerPickItemEvent.onMiddleClick() {
        if (VersionUtil.atleast("1.21.4")) return
        val baseEntity = FurnitureFactory.instance()?.packetManager()?.findTargetFurnitureHitbox(player) ?: return
        val mechanic = NexoFurniture.furnitureMechanic(baseEntity) ?: return
        val item = NexoItems.itemFromId(mechanic.itemID)?.build() ?: return

        for (i in 0..8) {
            if (player.inventory.getItem(i) == null) continue
            if (NexoItems.idFromItem(player.inventory.getItem(i)) != mechanic.itemID) continue

            player.inventory.heldItemSlot = i
            isCancelled = true
            return
        }

        isCancelled = true
        if (player.gameMode == GameMode.CREATIVE) player.inventory.setItemInMainHand(item)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun InventoryCreativeEvent.onMiddleClick() {
        if (VersionUtil.atleast("1.21.4")) return
        val player = inventory.holder as? Player ?: return
        if (clickedInventory == null || click != ClickType.CREATIVE) return
        if (slotType != InventoryType.SlotType.QUICKBAR || cursor.type != Material.BARRIER) return

        val baseEntity = FurnitureFactory.instance()?.packetManager()?.findTargetFurnitureHitbox(player) ?: return
        val mechanic = NexoFurniture.furnitureMechanic(baseEntity) ?: return
        val builder = NexoItems.itemFromId(mechanic.itemID) ?: return

        val item = builder.build()
        (0..8).forEach { i ->
            if (NexoItems.idFromItem(player.inventory.getItem(i)) != mechanic.itemID) return@forEach

            player.inventory.heldItemSlot = i
            isCancelled = true
            return
        }
        cursor = item
    }

    @EventHandler
    fun PlayerQuitEvent.onPlayerQuitEvent() {
        if (NexoFurniture.isFurniture(player.vehicle)) player.leaveVehicle()
    }

    @EventHandler
    fun EntityAddToWorldEvent.onInvalidFurniture() {
        if (entity !is ItemDisplay || !Settings.REMOVE_INVALID_FURNITURE.toBool()) return
        if (!entity.persistentDataContainer.has(FurnitureMechanic.FURNITURE_KEY) || NexoFurniture.isFurniture(entity)) return
        else entity.remove()
    }

    companion object {

        private fun getRotation(yaw: Double, mechanic: FurnitureMechanic): Rotation {
            val restrictedRotation = mechanic.restrictedRotation
            var id = (((Location.normalizeYaw(yaw.toFloat()) + 180) * 8 / 360) + 0.5).toInt() % 8
            val offset: Int = if (restrictedRotation == RestrictedRotation.STRICT) 0 else 1
            if (restrictedRotation != RestrictedRotation.NONE && id % 2 != 0) id -= offset
            return Rotation.entries[id]
        }
    }
}
