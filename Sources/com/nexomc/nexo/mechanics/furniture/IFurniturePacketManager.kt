package com.nexomc.nexo.mechanics.furniture

import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.mechanics.furniture.hitbox.BarrierHitbox
import com.nexomc.nexo.mechanics.light.LightBlock
import com.nexomc.nexo.utils.filterFastIsInstance
import com.nexomc.nexo.utils.flatMapFast
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import java.util.*

interface IFurniturePacketManager {
    fun nextEntityId(): Int
    fun getEntity(entityId: Int): Entity?

    fun sendFurnitureMetadataPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {}
    fun sendFurnitureMetadataPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player) {}

    fun sendInteractionEntityPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic)
    fun sendInteractionEntityPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player)
    fun removeInteractionHitboxPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic)
    fun removeInteractionHitboxPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player)

    fun sendBarrierHitboxPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic)
    fun sendBarrierHitboxPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player)
    fun removeBarrierHitboxPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic)
    fun removeBarrierHitboxPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player)

    fun sendLightMechanicPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {}
    fun sendLightMechanicPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player) {}
    fun removeLightMechanicPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {}
    fun removeLightMechanicPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player) {}

    fun removeAllFurniturePackets() {
        Bukkit.getWorlds().flatMapFast { it.entities }.filterFastIsInstance<ItemDisplay>().forEach { entity ->
            val mechanic = NexoFurniture.furnitureMechanic(entity) ?: return@forEach
            removeInteractionHitboxPacket(entity, mechanic)
            removeBarrierHitboxPacket(entity, mechanic)
            removeLightMechanicPacket(entity, mechanic)
        }
    }

    fun findTargetFurnitureHitbox(player: Player): ItemDisplay? {
        return null
    }

    companion object {
        val BARRIER_DATA = Material.BARRIER.createBlockData()
        val AIR_DATA = Material.AIR.createBlockData()

        val furnitureBaseMap = ObjectOpenHashSet<FurnitureBaseEntity>()
        val barrierHitboxPositionMap = Object2ObjectOpenHashMap<UUID, ObjectOpenHashSet<BarrierHitbox>>()
        val barrierHitboxLocationMap = Object2ObjectOpenHashMap<UUID, ObjectOpenHashSet<Location>>()

        val lightMechanicPositionMap = Object2ObjectOpenHashMap<UUID, ObjectOpenHashSet<LightBlock>>()
        val interactionHitboxIdMap = ObjectOpenHashSet<FurnitureSubEntity>()

        fun furnitureBaseFromBaseEntity(baseEntity: Entity): FurnitureBaseEntity? =
            furnitureBaseMap.firstOrNull { it.baseUuid == baseEntity.uniqueId }

        fun baseEntityFromFurnitureBase(furnitureBaseId: Int): ItemDisplay? =
            furnitureBaseMap.firstOrNull { it.baseId == furnitureBaseId }?.baseEntity()

        fun baseEntityFromHitbox(interactionId: Int): ItemDisplay? =
            interactionHitboxIdMap.firstOrNull { interactionId in it.entityIds }?.baseEntity()

        fun baseEntityFromHitbox(barrierLocation: BlockLocation): ItemDisplay? =
            barrierHitboxPositionMap.entries
                .firstOrNull { (_, hitboxes) -> hitboxes.any { it == barrierLocation } }
                ?.key?.let { uuid -> Bukkit.getEntity(uuid) as? ItemDisplay }

        fun standingOnFurniture(player: Player): Boolean {
            val playerLoc = BlockLocation(player.location)
            return barrierHitboxPositionMap.values.any { hitboxes ->
                hitboxes.any { it.distanceTo(playerLoc) <= 2 }
            }
        }

        fun blockIsHitbox(block: Block, excludeUUID: UUID? = null): Boolean =
            barrierHitboxLocationMap.any { (uuid, locations) -> uuid != excludeUUID && locations.any { it.equals(block.location) } }
    }

}
