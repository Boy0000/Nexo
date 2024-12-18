package com.nexomc.nexo.mechanics.furniture

import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.mechanics.furniture.hitbox.BarrierHitbox
import com.nexomc.nexo.mechanics.light.LightBlock
import com.nexomc.nexo.utils.ObservableMap
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

    fun sendFurnitureEntityPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {}
    fun sendFurnitureEntityPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player) {}
    fun removeFurnitureEntityPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {}
    fun removeFurnitureEntityPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player) {}

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
        Bukkit.getWorlds().flatMap { it.entities }.filterIsInstance<ItemDisplay>().forEach { entity ->
            val mechanic = NexoFurniture.furnitureMechanic(entity) ?: return@forEach
            removeFurnitureEntityPacket(entity, mechanic)
            removeLightMechanicPacket(entity, mechanic)
            removeInteractionHitboxPacket(entity, mechanic)
            removeBarrierHitboxPacket(entity, mechanic)
        }
    }

    companion object {
        val BARRIER_DATA = Material.BARRIER.createBlockData()
        val AIR_DATA = Material.AIR.createBlockData()

        val furnitureBaseMap = mutableSetOf<FurnitureBaseEntity>()
        val barrierHitboxPositionMap = ObservableMap<UUID, MutableSet<BarrierHitbox>>(mutableMapOf()) {
            barrierHitboxLocationMap = flatMap {
                val world = Bukkit.getEntity(it.key)?.world ?: Bukkit.getWorlds().first()
                it.value.map { b -> b.toLocation(world) }
            }.toSet()
        }

        var barrierHitboxLocationMap: Set<Location> = emptySet()

        val lightMechanicPositionMap = mutableMapOf<UUID, MutableSet<LightBlock>>()
        val interactionHitboxIdMap = mutableSetOf<FurnitureSubEntity>()

        fun furnitureBaseFromBaseEntity(baseEntity: Entity) =
            Optional.ofNullable(furnitureBaseMap.firstOrNull { it.baseUuid === baseEntity.uniqueId })

        fun baseEntityFromFurnitureBase(furnitureBaseId: Int) =
            furnitureBaseMap.firstOrNull { it.baseId == furnitureBaseId }?.baseEntity()

        fun baseEntityFromHitbox(interactionId: Int) =
            interactionHitboxIdMap.firstOrNull { interactionId in it.entityIds }?.baseEntity()

        fun baseEntityFromHitbox(barrierLocation: BlockLocation): ItemDisplay? {
            barrierHitboxPositionMap.filterValues { it.any(barrierLocation::equals) }.keys.forEach { uuid ->
                return Bukkit.getEntity(uuid) as? ItemDisplay ?: return@forEach
            }
            return null
        }

        fun standingOnFurniture(player: Player): Boolean {
            val playerLoc = BlockLocation(player.location)
            return barrierHitboxPositionMap.values.flatten().any { it.distanceTo(playerLoc) <= 2 }
        }

        fun blockIsHitbox(block: Block, excludeUUID: UUID? = null): Boolean {
            return barrierHitboxPositionMap.filterKeys { it != excludeUUID }.values.flatten().any { it == BlockLocation(block.location) }
        }
    }
}
