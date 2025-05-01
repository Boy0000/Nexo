package com.nexomc.nexo.api

import com.nexomc.nexo.mechanics.furniture.BlockLocation
import com.nexomc.nexo.mechanics.furniture.FurnitureFactory
import com.nexomc.nexo.mechanics.furniture.FurnitureHelpers
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager.Companion.furnitureBaseMap
import com.nexomc.nexo.mechanics.furniture.bed.FurnitureBed
import com.nexomc.nexo.mechanics.furniture.seats.FurnitureSeat
import com.nexomc.nexo.utils.BlockHelpers.isLoaded
import com.nexomc.nexo.utils.BlockHelpers.toCenterBlockLocation
import com.nexomc.nexo.utils.SchedulerUtils
import com.nexomc.nexo.utils.drops.Drop
import org.bukkit.GameEvent
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Rotation
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object NexoFurniture {
    /**
     * Get all NexoItem IDs that have a FurnitureMechanic
     *
     * @return a Set of all NexoItem IDs that have a FurnitureMechanic
     */
    @JvmStatic
    fun furnitureIDs(): Array<String> = NexoItems.itemNames().filter(::isFurniture).toTypedArray()
    /**
     * Check if a location contains a Furniture
     *
     * @param location The location to check
     * @return true if the location contains a Furniture, otherwise false
     */
    @JvmStatic
    fun isFurniture(location: Location): Boolean {
        return (furnitureMechanic(location) != null) || location.getWorld().getNearbyEntitiesByType(ItemDisplay::class.java, location.toCenterLocation(), 0.5).any(::isFurniture)
    }

    /**
     * Check if an itemID has a FurnitureMechanic
     *
     * @param itemID The itemID to check
     * @return true if the itemID has a FurnitureMechanic, otherwise false
     */
    @JvmStatic
    fun isFurniture(itemID: String?) = FurnitureFactory.instance()?.isNotImplementedIn(itemID) == false

    @JvmStatic
    fun isFurniture(itemStack: ItemStack?) = isFurniture(NexoItems.idFromItem(itemStack))

    @JvmStatic
    fun isFurniture(entity: Entity?) = entity?.type == EntityType.ITEM_DISPLAY && furnitureMechanic(entity) != null

    @JvmStatic
    fun baseEntity(block: Block?): ItemDisplay? = FurnitureMechanic.baseEntity(block)

    @JvmStatic
    fun baseEntity(location: Location?): ItemDisplay? = FurnitureMechanic.baseEntity(location)

    @JvmStatic
    fun baseEntity(interactionId: Int) = FurnitureMechanic.baseEntity(interactionId)

    /**
     * Places Furniture at a given location
     * @param location The location to place the Furniture
     * @param itemID The itemID of the Furniture to place
     * @param rotation The rotation of the Furniture
     * @param blockFace The blockFace of the Furniture
     * @return The Furniture entity that was placed, or null if the Furniture could not be placed
     */
    @JvmStatic
    fun place(itemID: String?, location: Location, rotation: Rotation, blockFace: BlockFace): ItemDisplay? {
        return place(itemID, location, FurnitureHelpers.rotationToYaw(rotation), blockFace)
    }

    /**
     * Places Furniture at a given location
     * @param location The location to place the Furniture
     * @param itemID The itemID of the Furniture to place
     * @param yaw The yaw of the Furniture
     * @param blockFace The blockFace of the Furniture
     * @return The Furniture entity that was placed, or null if the Furniture could not be placed
     */
    @JvmStatic
    fun place(itemID: String?, location: Location, yaw: Float, blockFace: BlockFace): ItemDisplay? {
        val mechanic = furnitureMechanic(itemID) ?: return null
        return mechanic.place(location, yaw, blockFace)
    }

    /**
     * Removes Furniture at a given location, optionally by a player
     *
     * @param location The location to remove the Furniture
     * @param player   The player who removed the Furniture, can be null
     * @param drop     The drop of the furniture, if null the default drop will be used
     * @return true if the Furniture was removed, false otherwise
     */
    @JvmOverloads
    @JvmStatic
    fun remove(location: Location, player: Player? = null, drop: Drop? = null): Boolean {
        if (!FurnitureFactory.isEnabled || !location.isLoaded) return false

        val mechanic = furnitureMechanic(location) ?: location.world.getNearbyEntitiesByType(ItemDisplay::class.java, location, 0.5).firstNotNullOfOrNull(::furnitureMechanic) ?: return false
        val itemStack = player?.inventory?.itemInMainHand ?: ItemStack(Material.AIR)
        val baseEntity = FurnitureMechanic.baseEntity(location) ?: return false

        if (player != null) {
            if (player.gameMode != GameMode.CREATIVE) (drop ?: mechanic.breakable.drop).furnitureSpawns(baseEntity, itemStack)
            mechanic.storage?.takeIf { it.isStorage || it.isShulker }?.dropStorageContent(mechanic, baseEntity)
            baseEntity.world.sendGameEvent(player, GameEvent.BLOCK_DESTROY, baseEntity.location.toVector())
        }

        mechanic.removeBaseEntity(baseEntity)
        return true
    }

    /**
     * Removes Furniture at a given Entity, optionally by a player and with an altered Drop
     *
     * @param baseEntity The entity at which the Furniture should be removed
     * @param player     The player who removed the Furniture, can be null
     * @param drop       The drop of the furniture, if null the default drop will be used
     * @return true if the Furniture was removed, false otherwise
     */
    @JvmOverloads
    @JvmStatic
    fun remove(baseEntity: Entity, player: Player? = null, drop: Drop? = null): Boolean {
        val mechanic = (baseEntity as? ItemDisplay)?.let(::furnitureMechanic) ?: return false

        // Allows for changing the FurnitureType in config and still remove old entities
        if (player != null) {
            val itemStack = player.inventory.itemInMainHand
            if (player.gameMode != GameMode.CREATIVE) (drop ?: mechanic.breakable.drop).furnitureSpawns(baseEntity, itemStack)
            mechanic.storage?.takeIf { it.isStorage || it.isShulker }?.dropStorageContent(mechanic, baseEntity)
            baseEntity.world.sendGameEvent(player, GameEvent.BLOCK_DESTROY, baseEntity.location.toVector())
        }

        // Check if the mechanic or the baseEntity has barriers tied to it
        mechanic.removeBaseEntity(baseEntity)
        return true
    }

    @JvmStatic
    fun furnitureMechanic(block: Block?): FurnitureMechanic? {
        return furnitureMechanic(block?.location)
    }

    /**
     * Get the FurnitureMechanic from a given location.
     * This will only return non-null for furniture with a barrier-hitbox
     *
     * @param location The location to get the FurnitureMechanic from
     * @return Instance of this block's FurnitureMechanic, or null if the location is not tied to a Furniture
     */
    @JvmStatic
    fun furnitureMechanic(location: Location?): FurnitureMechanic? {
        if (!FurnitureFactory.isEnabled || location == null) return null
        return IFurniturePacketManager.baseEntityFromHitbox(BlockLocation(location))?.let(::furnitureMechanic) ?: let {
            val block = location.block
            val centerLoc = toCenterBlockLocation(location)
            centerLoc.world.getNearbyEntitiesByType(ItemDisplay::class.java, centerLoc, 2.0)
                .sortedBy { it.location.distanceSquared(centerLoc) }
                .firstOrNull { IFurniturePacketManager.blockIsHitbox(block) }
                ?.let(::furnitureMechanic)
        }
    }

    /**
     * Get the FurnitureMechanic from a given entity.
     *
     * @param baseEntity The entity to get the FurnitureMechanic from
     * @return Returns this entity's FurnitureMechanic, or null if the entity is not tied to a Furniture
     */
    @JvmStatic
    fun furnitureMechanic(baseEntity: Entity?): FurnitureMechanic? {
        if (!FurnitureFactory.isEnabled || baseEntity == null || baseEntity.type != EntityType.ITEM_DISPLAY || (!baseEntity.isValid && !baseEntity.isInWorld)) return null
        val itemID = baseEntity.persistentDataContainer.get(FurnitureMechanic.FURNITURE_KEY, PersistentDataType.STRING)
        if (!NexoItems.exists(itemID) || FurnitureSeat.isSeat(baseEntity)|| FurnitureBed.isBed(baseEntity)) return null
        // Ignore legacy hitbox entities as they should be removed in FurnitureConverter
        if (baseEntity is Interaction) return null
        return FurnitureFactory.instance()?.getMechanic(itemID)
    }

    /**
     * Get the FurnitureMechanic from a given block.
     * This will only return non-null for furniture with a barrier-hitbox
     *
     * @param itemID The itemID tied to this FurnitureMechanic
     * @return Returns the FurnitureMechanic tied to this itemID, or null if the itemID is not tied to a Furniture
     */
    @JvmStatic
    fun furnitureMechanic(itemID: String?): FurnitureMechanic? {
        if (!FurnitureFactory.isEnabled || !NexoItems.exists(itemID)) return null
        return FurnitureFactory.instance()?.getMechanic(itemID)
    }

    /**
     * Ensures that the given entity is a Furniture, and updates it if it is
     *
     * @param baseEntity The furniture baseEntity to update
     */
    @JvmStatic
    fun updateFurniture(baseEntity: ItemDisplay) {
        if (!FurnitureFactory.isEnabled || !baseEntity.location.isLoaded) return
        val mechanic = furnitureMechanic(baseEntity)?.takeUnless { FurnitureSeat.isSeat(baseEntity)|| FurnitureBed.isBed(baseEntity) } ?: return

        FurnitureSeat.updateSeats(baseEntity, mechanic)
        FurnitureBed.updateBeds(baseEntity, mechanic)

        val packetManager = FurnitureFactory.instance()?.packetManager() ?: return
        furnitureBaseMap.remove(baseEntity.uniqueId)
        packetManager.removeLightMechanicPacket(baseEntity, mechanic)
        packetManager.removeHitboxEntityPacket(baseEntity, mechanic)
        packetManager.removeBarrierHitboxPacket(baseEntity, mechanic)

        SchedulerUtils.foliaScheduler.runAtEntity(baseEntity) {
            packetManager.sendFurnitureMetadataPacket(baseEntity, mechanic)
            packetManager.sendHitboxEntityPacket(baseEntity, mechanic)
            packetManager.sendBarrierHitboxPacket(baseEntity, mechanic)
            packetManager.sendLightMechanicPacket(baseEntity, mechanic)
        }
    }

    @JvmStatic
    fun findTargetFurniture(player: Player): ItemDisplay? {
        return FurnitureFactory.instance()?.packetManager()?.findTargetFurnitureHitbox(player)
    }
}
