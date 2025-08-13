package com.nexomc.nexo.mechanics.furniture

import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.mechanics.furniture.hitbox.BarrierHitbox
import com.nexomc.nexo.mechanics.light.LightBlock
import com.nexomc.nexo.utils.SchedulerUtils
import com.nexomc.nexo.utils.printOnFailure
import com.nexomc.nexo.utils.to
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.data.Waterlogged
import org.bukkit.entity.Entity
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import java.util.*

interface IFurniturePacketManager {
    fun getEntity(entityId: Int): Entity?

    fun sendFurnitureMetadataPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {}
    fun sendFurnitureMetadataPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player) {}

    fun sendHitboxEntityPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic)
    fun sendHitboxEntityPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player)
    fun removeHitboxEntityPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic)
    fun removeHitboxEntityPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player)

    fun sendBarrierHitboxPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic)
    fun sendBarrierHitboxPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player)
    fun removeBarrierHitboxPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic)
    fun removeBarrierHitboxPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player)

    fun sendLightMechanicPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {}
    fun sendLightMechanicPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player) {}
    fun removeLightMechanicPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {}
    fun removeLightMechanicPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player) {}

    fun removeAllFurniturePackets() {
        SchedulerUtils.runAtWorldEntities<ItemDisplay> { entity ->
            val mechanic = NexoFurniture.furnitureMechanic(entity) ?: return@runAtWorldEntities
            removeHitboxEntityPacket(entity, mechanic)
            removeBarrierHitboxPacket(entity, mechanic)
            removeLightMechanicPacket(entity, mechanic)
        }
    }

    fun findTargetFurnitureHitbox(player: Player): ItemDisplay? {
        return null
    }

    companion object {
        val BARRIER_DATA = Material.BARRIER.createBlockData() as Waterlogged
        val BARRIER_DATA_WATERLOGGED = (Material.BARRIER.createBlockData() as Waterlogged).apply { isWaterlogged = true }
        val AIR_DATA = Material.AIR.createBlockData()
        val WATER_DATA = Material.WATER.createBlockData()

        val furnitureBaseMap = Object2ObjectOpenHashMap<UUID, FurnitureBaseEntity>()
        val barrierHitboxPositionMap = Object2ObjectOpenHashMap<UUID, Array<BarrierHitbox>>()
        val barrierHitboxLocationMap = Object2ObjectOpenHashMap<UUID, Array<Location>>()

        val lightPositionMap = Object2ObjectOpenHashMap<UUID, Array<LightBlock>>()
        val lightLocationMap = Object2ObjectOpenHashMap<UUID, Array<Location>>()

        val interactionHitboxPacketMap: Object2ObjectOpenHashMap<UUID, Array<IFurniturePacket>> = Object2ObjectOpenHashMap()
        val shulkerHitboxPacketMap: Object2ObjectOpenHashMap<UUID, Array<IFurniturePacket>> = Object2ObjectOpenHashMap()
        val ghastHitboxPacketMap: Object2ObjectOpenHashMap<UUID, Array<IFurniturePacket>> = Object2ObjectOpenHashMap()

        val interactionHitboxIdMap = Object2ObjectOpenHashMap<UUID, FurnitureSubEntity>()
        val shulkerHitboxIdMap = Object2ObjectOpenHashMap<UUID, FurnitureSubEntity>()
        val ghastHitboxIdMap = Object2ObjectOpenHashMap<UUID, FurnitureSubEntity>()

        fun updateBaseEntity(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {
            furnitureBaseMap.get(baseEntity.uniqueId)?.mechanic = mechanic
        }

        fun baseEntityFromHitbox(entityId: Int): ItemDisplay? =
            interactionHitboxIdMap.values.find { entityId in it.entityIds }?.baseEntity()
                ?: shulkerHitboxIdMap.values.find { entityId in it.entityIds }?.baseEntity()
                ?: ghastHitboxIdMap.values.find { entityId in it.entityIds }?.baseEntity()

        fun baseEntityFromHitbox(location: Location): ItemDisplay? {
            return baseEntityFromHitbox(BlockLocation(location), location.world)
        }

        fun baseEntityFromHitbox(location: BlockLocation, world: World): ItemDisplay? {
            val barrierVec = location.toVector()
            return barrierHitboxPositionMap.firstNotNullOfOrNull { (uuid, hitboxes) ->
                world.takeIf { hitboxes.any { it == location } }?.getEntity(uuid) as? ItemDisplay
            } ?: interactionHitboxIdMap.firstNotNullOfOrNull { subEntity ->
                world.takeIf { subEntity.value.boundingBoxes.any { it.contains(barrierVec) } }?.getEntity(subEntity.value.baseUuid) as? ItemDisplay
            } ?: shulkerHitboxIdMap.firstNotNullOfOrNull { subEntity ->
                world.takeIf { subEntity.value.boundingBoxes.any { it.contains(barrierVec) } }?.getEntity(subEntity.value.baseUuid) as? ItemDisplay
            } ?: ghastHitboxIdMap.firstNotNullOfOrNull { subEntity ->
                world.takeIf { subEntity.value.boundingBoxes.any { it.contains(barrierVec) } }?.getEntity(subEntity.value.baseUuid) as? ItemDisplay
            }
        }

        fun baseEntityFromLight(location: BlockLocation, world: World): ItemDisplay? {
            return lightPositionMap.firstNotNullOfOrNull { (uuid, hitboxes) ->
                world.takeIf { hitboxes.any { it == location } }?.getEntity(uuid) as? ItemDisplay
            }
        }

        fun mechanicFromHitbox(entityId: Int): FurnitureMechanic? =
            interactionHitboxIdMap.values.find { entityId in it.entityIds }?.mechanic()
                ?: shulkerHitboxIdMap.values.find { entityId in it.entityIds }?.mechanic()
                ?: ghastHitboxIdMap.values.find { entityId in it.entityIds }?.mechanic()

        fun mechanicFromHitbox(location: BlockLocation): FurnitureMechanic? {
            val barrierVec = location.toVector()
            return barrierHitboxPositionMap.firstNotNullOfOrNull { (uuid, hitboxes) ->
                furnitureBaseMap.takeIf { location in hitboxes }?.get(uuid)?.mechanic()
            } ?: interactionHitboxIdMap.firstNotNullOfOrNull { subEntity ->
                subEntity.takeIf { subEntity.value.boundingBoxes.any { it.contains(barrierVec) } }?.value?.mechanic()
            } ?: shulkerHitboxIdMap.firstNotNullOfOrNull { subEntity ->
                subEntity.takeIf { subEntity.value.boundingBoxes.any { it.contains(barrierVec) } }?.value?.mechanic()
            } ?: ghastHitboxIdMap.firstNotNullOfOrNull { subEntity ->
                subEntity.takeIf { subEntity.value.boundingBoxes.any { it.contains(barrierVec) } }?.value?.mechanic()
            }
        }

        fun hitboxLocFromId(entityId: Int, world: World): Location? {
            val subEntity = interactionHitboxIdMap.values.find { entityId in it.entityIds }
                ?: shulkerHitboxIdMap.values.find { entityId in it.entityIds }
                ?: ghastHitboxIdMap.values.find { entityId in it.entityIds }
                ?: return null
            return subEntity.hitboxLocation(entityId)?.toLocation(world)
        }

        fun standingOnFurniture(player: Player): Boolean {
            val playerLoc = BlockLocation(player.location)
            return barrierHitboxPositionMap.values.any { hitboxes ->
                hitboxes.any { it.distanceTo(playerLoc) <= 3 }
            }
        }

        fun blockIsHitbox(vec: Vector, world: World, excludeUUID: UUID? = null): Boolean {
            return runCatching { barrierHitboxLocationMap.any { (uuid, locations) -> uuid != excludeUUID && locations.any {
                it.x == vec.x && it.y == vec.y && it.z == vec.z && it.world == world
            } } }.printOnFailure().getOrDefault(false)
        }

        fun blockIsHitbox(block: Block, excludeUUID: UUID? = null, collisionOnly: Boolean = true): Boolean {
            if (barrierHitboxLocationMap.any { (uuid, locations) -> uuid != excludeUUID && locations.any(block.location::equals) })
                return true
            if (collisionOnly) return false
            val blockBox = BoundingBox.of(block)
            return interactionHitboxIdMap.values.any { it.baseUuid != excludeUUID && it.boundingBoxes.any(blockBox::overlaps) }
                    || shulkerHitboxIdMap.values.any { it.baseUuid != excludeUUID && it.boundingBoxes.any(blockBox::overlaps) }
                    || ghastHitboxIdMap.values.any { it.baseUuid != excludeUUID && it.boundingBoxes.any(blockBox::overlaps) }
        }

        fun blockIsHitbox(location: Location, excludeUUID: UUID? = null): Boolean {
            val (x, y, z, world) = location.blockX to location.blockY to location.blockZ to location.world
            val blockBox = BoundingBox.of(location.toCenterLocation(), 0.5, 0.5, 0.5)
            return barrierHitboxLocationMap.any { (uuid, locations) -> uuid != excludeUUID && locations.any { it.blockX == x && it.blockY == y && it.blockZ == z && it.world == world } }
                    || interactionHitboxIdMap.values.any { it.baseUuid != excludeUUID && it.boundingBoxes.any(blockBox::overlaps) }
                    || shulkerHitboxIdMap.values.any { it.baseUuid != excludeUUID && it.boundingBoxes.any(blockBox::overlaps) }
                    || ghastHitboxIdMap.values.any { it.baseUuid != excludeUUID && it.boundingBoxes.any(blockBox::overlaps) }
        }

        fun blockIsLight(vec: Vector, world: World, excludeUUID: UUID? = null): Boolean {
            return runCatching { lightLocationMap.any { (uuid, locations) -> uuid != excludeUUID && locations.any {
                it.x == vec.x && it.y == vec.y && it.z == vec.z && it.world == world
            } } }.printOnFailure().getOrDefault(false)
        }
    }
}
