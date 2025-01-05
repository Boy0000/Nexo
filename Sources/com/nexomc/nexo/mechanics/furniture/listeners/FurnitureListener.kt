package com.nexomc.nexo.mechanics.furniture.listeners

import com.jeff_media.morepersistentdatatypes.DataType
import com.nexomc.nexo.utils.to
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent
import com.nexomc.nexo.api.events.furniture.NexoFurniturePlaceEvent
import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.mechanics.furniture.*
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic.RestrictedRotation
import com.nexomc.nexo.mechanics.furniture.seats.FurnitureSeat
import com.nexomc.nexo.mechanics.limitedplacing.LimitedPlacing.LimitedPlacingType
import com.nexomc.nexo.mechanics.storage.StorageType
import com.nexomc.nexo.utils.BlockHelpers
import com.nexomc.nexo.utils.EventUtils.call
import com.nexomc.nexo.utils.ItemUtils.dyeColor
import com.nexomc.nexo.utils.Utils.swingHand
import com.nexomc.nexo.utils.VersionUtil
import io.papermc.paper.event.entity.EntityMoveEvent
import io.th0rgal.protectionlib.ProtectionLib
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockFormEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryCreativeEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.EntitiesLoadEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class FurnitureListener : Listener {
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun PlayerInteractEvent.onLimitedPlacing() {
        val (block, itemId) = (clickedBlock ?: return) to (item?.let(NexoItems::idFromItem) ?: return)

        if (hand != EquipmentSlot.HAND || action != Action.RIGHT_CLICK_BLOCK) return
        if (!player.isSneaking && BlockHelpers.isInteractable(clickedBlock)) return

        val mechanic = NexoFurniture.furnitureMechanic(itemId) ?: return
        val limitedPlacing = mechanic.limitedPlacing ?: return
        val belowPlaced = block.getRelative(blockFace).getRelative(BlockFace.DOWN)

        when {
            limitedPlacing.isNotPlacableOn(block, blockFace) -> isCancelled = true
            limitedPlacing.type == LimitedPlacingType.ALLOW && !limitedPlacing.checkLimited(belowPlaced) ->
                isCancelled = true
            limitedPlacing.type == LimitedPlacingType.DENY && limitedPlacing.checkLimited(belowPlaced) ->
                isCancelled = true
            limitedPlacing.isRadiusLimited -> {
                val (radius, amount) = limitedPlacing.radiusLimitation!!.let { it.radius.toDouble() to it.amount.toDouble()}
                if (block.world.getNearbyEntities(block.location, radius, radius, radius)
                        .filter { NexoFurniture.furnitureMechanic(it)?.itemID == mechanic.itemID }
                        .count { it.location.distanceSquared(block.location) <= radius * radius } >= amount
                ) isCancelled = true
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    fun PlayerInteractEvent.onFurniturePlace() {
        val block = clickedBlock?.let { if (BlockHelpers.isReplaceable(it)) it else it.getRelative(blockFace) } ?: return
        val (item, hand) = (item ?: return) to (hand?.takeIf { it == EquipmentSlot.HAND } ?: return)
        val mechanic = FurnitureFactory.instance()?.getMechanic(item) ?: return

        if (action != Action.RIGHT_CLICK_BLOCK || (useInteractedBlock() == Event.Result.DENY && !player.isSneaking) || useItemInHand() == Event.Result.DENY) return
        if (!NexoFurniture.isFurniture(item) || BlockHelpers.isStandingInside(player, block)) return
        if (!player.isSneaking && BlockHelpers.isInteractable(clickedBlock)) return
        if (!ProtectionLib.canBuild(player, block.location)) return
        if (!player.isSneaking && NexoFurniture.furnitureMechanic(block)?.isInteractable == true) return
        if (mechanic.farmlandRequired && block.getRelative(BlockFace.DOWN).type != Material.FARMLAND) return


        val currentBlockData = block.blockData
        val blockPlaceEvent = BlockPlaceEvent(block, block.state, block.getRelative(blockFace), item, player, true, hand)
        val rotation = getRotation(player.eyeLocation.yaw.toDouble(), mechanic)
        val yaw = FurnitureHelpers.correctedYaw(mechanic, FurnitureHelpers.rotationToYaw(rotation))

        if (player.gameMode == GameMode.ADVENTURE) blockPlaceEvent.isCancelled = true
        if (mechanic.notEnoughSpace(block.location, yaw)) {
            blockPlaceEvent.isCancelled = true
            Message.NOT_ENOUGH_SPACE.send(player)
        }

        if (!blockPlaceEvent.canBuild() || !blockPlaceEvent.call()) return

        val baseEntity = mechanic.place(block.location, yaw, blockFace, false) ?: return

        dyeColor(item).ifPresent { color: Color ->
            baseEntity.persistentDataContainer.set(FurnitureMechanic.FURNITURE_DYE_KEY, PersistentDataType.INTEGER, color.asRGB())
        }
        swingHand(player, hand)

        if (!NexoFurniturePlaceEvent(mechanic, block, baseEntity, player, item, hand).call()) {
            NexoFurniture.remove(baseEntity)
            block.blockData = currentBlockData
            return
        }

        if (player.gameMode != GameMode.CREATIVE) item.amount -= 1
        setUseInteractedBlock(Event.Result.DENY)
        if (VersionUtil.isPaperServer) baseEntity.world.sendGameEvent(player, GameEvent.BLOCK_PLACE, baseEntity.location.toVector())
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
        val mechanic = NexoFurniture.furnitureMechanic(baseEntity) ?: return
        if (itemStack.type.hasGravity() || isItemFrame || isArmorStand || isPainting) {
            setUseItemInHand(Event.Result.DENY)
            player.updateInventory()
            return
        }

        // Since the server-side block is AIR by default, placing blocks acts weird
        // Temporarily set the block to a barrier, then schedule a task to revert it next tick and resend hitboxes
        block.setType(Material.BARRIER, false)
        Bukkit.getScheduler().scheduleSyncDelayedTask(NexoPlugin.instance()) { block.setType(Material.AIR, false) }
        Bukkit.getScheduler().scheduleSyncDelayedTask(
            NexoPlugin.instance(), {
                FurnitureFactory.instance()?.packetManager()?.sendBarrierHitboxPacket(baseEntity, mechanic, player)
                FurnitureFactory.instance()?.packetManager()?.sendLightMechanicPacket(baseEntity, mechanic, player)
            }, 1L
        )
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun NexoFurnitureInteractEvent.onFurnitureInteract() {
        val allowUseItem = useItemInHand
        useItemInHand = Event.Result.DENY

        if (hand == EquipmentSlot.HAND && useFurniture != Event.Result.DENY) {
            if (mechanic.clickActions.isNotEmpty()) mechanic.runClickActions(player)
            if (mechanic.lightIsToggleable) FurnitureHelpers.toggleLight(baseEntity)

            when {
                mechanic.rotatable.rotatable && (!mechanic.rotatable.onSneak || player.isSneaking) -> mechanic.rotateFurniture(baseEntity)
                mechanic.isStorage() && !player.isSneaking -> {
                    val storage = mechanic.storage ?: return
                    when (storage.storageType) {
                        StorageType.STORAGE, StorageType.SHULKER -> storage.openStorage(baseEntity, player)
                        StorageType.PERSONAL -> storage.openPersonalStorage(player, baseEntity.location, baseEntity)
                        StorageType.DISPOSAL -> storage.openDisposal(player, baseEntity.location, baseEntity)
                        StorageType.ENDERCHEST -> player.openInventory(player.enderChest)
                    }
                }
                mechanic.hasSeats() && !player.isSneaking -> FurnitureSeat.sitOnSeat(baseEntity, player, interactionPoint)
                mechanic.clickActions.isEmpty() && !mechanic.lightIsToggleable -> useItemInHand = allowUseItem
            }
        }

        player.updateInventory()

        Bukkit.getScheduler().scheduleSyncDelayedTask(
            NexoPlugin.instance(), {
                FurnitureFactory.instance()?.packetManager()?.sendBarrierHitboxPacket(baseEntity, mechanic, player)
                FurnitureFactory.instance()?.packetManager()?.sendLightMechanicPacket(baseEntity, mechanic, player)
        }, 1L)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun InventoryCreativeEvent.onMiddleClick() {
        val player = inventory.holder as Player?
        if (clickedInventory == null || player == null) return
        if (click != ClickType.CREATIVE) return
        if (slotType != InventoryType.SlotType.QUICKBAR) return
        if (cursor.type != Material.BARRIER) return

        val baseEntity = FurnitureFactory.instance()?.packetManager()?.findTargetFurnitureHitbox(player) ?: return
        val mechanic = NexoFurniture.furnitureMechanic(baseEntity) ?: return
        val builder = NexoItems.itemFromId(mechanic.itemID) ?: return

        val item = builder.build()
        (0..8).forEach { i ->
            if (NexoItems.idFromItem(player.inventory.getItem(i)) == mechanic.itemID) {
                player.inventory.heldItemSlot = i
                isCancelled = true
                return
            }
        }
        cursor = item
    }

    @EventHandler
    fun PlayerQuitEvent.onPlayerQuitEvent() {
        if (NexoFurniture.isFurniture(player.vehicle)) player.leaveVehicle()
    }

    @EventHandler
    fun EntitiesLoadEvent.onInvalidFurniture() {
        if (Settings.REMOVE_INVALID_FURNITURE.toBool()) entities.filterIsInstance<ItemDisplay>().forEach {
            if (!it.persistentDataContainer.has(FurnitureMechanic.FURNITURE_KEY) || NexoFurniture.isFurniture(it)) return@forEach
            it.remove()
        }
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
