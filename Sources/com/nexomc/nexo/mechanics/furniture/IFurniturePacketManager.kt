package com.nexomc.nexo.mechanics.furniture

import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.mechanics.furniture.hitbox.BarrierHitbox
import com.nexomc.nexo.mechanics.light.LightBlock
import com.nexomc.nexo.utils.SchedulerUtils
import com.nexomc.nexo.utils.printOnFailure
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import java.util.UUID
import org.bukkit.Bukkit
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

interface IFurniturePacketManager {
    fun nextEntityId(): Int
    fun getEntity(entityId: Int): Entity?

    fun sendFurnitureMetadataPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {}
    fun sendFurnitureMetadataPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player) {}

    fun sendInteractionEntityPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic)
    fun sendInteractionEntityPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player)
    fun removeInteractionHitboxPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic)
    fun removeInteractionHitboxPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player)

    fun sendShulkerEntityPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic)
    fun sendShulkerEntityPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player)
    fun removeShulkerHitboxPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic)
    fun removeShulkerHitboxPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player)

    fun sendBarrierHitboxPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic)
    fun sendBarrierHitboxPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player)
    fun removeBarrierHitboxPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic)
    fun removeBarrierHitboxPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player)

    fun sendLightMechanicPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {}
    fun sendLightMechanicPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player) {}
    fun removeLightMechanicPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {}
    fun removeLightMechanicPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player) {}

    fun removeAllFurniturePackets() {
        SchedulerUtils.runAtWorldEntities { entity ->
            val mechanic = (entity as? ItemDisplay)?.let(NexoFurniture::furnitureMechanic) ?: return@runAtWorldEntities
            removeInteractionHitboxPacket(entity, mechanic)
            removeShulkerHitboxPacket(entity, mechanic)
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

        val furnitureBaseMap = ObjectOpenHashSet<FurnitureBaseEntity>()
        val barrierHitboxPositionMap = Object2ObjectOpenHashMap<UUID, Array<BarrierHitbox>>()
        val barrierHitboxLocationMap = Object2ObjectOpenHashMap<UUID, Array<Location>>()

        val lightMechanicPositionMap = Object2ObjectOpenHashMap<UUID, Array<LightBlock>>()
        val interactionHitboxIdMap = mutableSetOf<FurnitureSubEntity>()
        val shulkerHitboxIdMap = mutableSetOf<FurnitureSubEntity>()

        fun furnitureBaseFromBaseEntity(baseEntity: Entity): FurnitureBaseEntity? =
            furnitureBaseMap.firstOrNull { it.baseUuid == baseEntity.uniqueId }

        fun baseEntityFromFurnitureBase(furnitureBaseId: Int): ItemDisplay? =
            furnitureBaseMap.firstOrNull { it.baseId == furnitureBaseId }?.baseEntity()

        fun baseEntityFromHitbox(entityId: Int): ItemDisplay? =
            interactionHitboxIdMap.find { entityId in it.entityIds }?.baseEntity()
                ?: shulkerHitboxIdMap.find { entityId in it.entityIds }?.baseEntity()

        fun baseEntityFromHitbox(location: BlockLocation): ItemDisplay? {
            val barrierVec = location.toVector()
            return barrierHitboxPositionMap.entries.firstNotNullOfOrNull { (uuid, hitboxes) ->
                uuid.takeIf { hitboxes.any { it == location } }?.let(Bukkit::getEntity) as? ItemDisplay
            } ?: interactionHitboxIdMap.firstNotNullOfOrNull { subEntity ->
                subEntity.takeIf { subEntity.boundingBoxes.any { it.contains(barrierVec) } }?.baseEntity()
            } ?: shulkerHitboxIdMap.firstNotNullOfOrNull { subEntity ->
                subEntity.takeIf { subEntity.boundingBoxes.any { it.contains(barrierVec) } }?.baseEntity()
            }
        }

        fun mechanicFromHitbox(entityId: Int): FurnitureMechanic? =
            interactionHitboxIdMap.find { entityId in it.entityIds }?.mechanic()
                ?: shulkerHitboxIdMap.find { entityId in it.entityIds }?.mechanic()

        fun mechanicFromHitbox(location: BlockLocation): FurnitureMechanic? {
            val barrierVec = location.toVector()
            return barrierHitboxPositionMap.entries.firstNotNullOfOrNull { (uuid, hitboxes) ->
                furnitureBaseMap.takeIf { location in hitboxes }?.firstOrNull { it.baseUuid == uuid }?.mechanic()
            } ?: interactionHitboxIdMap.firstNotNullOfOrNull { subEntity ->
                subEntity.takeIf { subEntity.boundingBoxes.any { it.contains(barrierVec) } }?.mechanic()
            } ?: shulkerHitboxIdMap.firstNotNullOfOrNull { subEntity ->
                subEntity.takeIf { subEntity.boundingBoxes.any { it.contains(barrierVec) } }?.mechanic()
            }
        }

        fun hitboxLocFromId(entityId: Int, world: World): Location? {
            val subEntity = interactionHitboxIdMap.find { entityId in it.entityIds } ?: shulkerHitboxIdMap.find { entityId in it.entityIds } ?: return null
            return subEntity.hitboxLocation(entityId)?.toLocation(world)
        }

        fun standingOnFurniture(player: Player): Boolean {
            val playerLoc = BlockLocation(player.location)
            return barrierHitboxPositionMap.values.any { hitboxes ->
                hitboxes.any { it.distanceTo(playerLoc) <= 3 }
            }
        }

        fun blockIsHitbox(vec: Vector, world: World, excludeUUID: UUID? = null): Boolean {
            return runCatching { barrierHitboxLocationMap.any { (uuid, locations) -> uuid != excludeUUID && locations.filterNotNull().any {
                (it.toVector() == vec)
            } } }.printOnFailure().isSuccess
        }

        fun blockIsHitbox(block: Block, excludeUUID: UUID? = null): Boolean {
            val blockBox = BoundingBox.of(block)
            return barrierHitboxLocationMap.any { (uuid, locations) -> uuid != excludeUUID && locations.any(block.location::equals) }
                    || interactionHitboxIdMap.any { it.baseUuid != excludeUUID && it.boundingBoxes.any(blockBox::overlaps) }
                    || shulkerHitboxIdMap.any { it.baseUuid != excludeUUID && it.boundingBoxes.any(blockBox::overlaps) }
        }
    }

}
