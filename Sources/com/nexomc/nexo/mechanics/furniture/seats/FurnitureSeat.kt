package com.nexomc.nexo.mechanics.furniture.seats

import com.jeff_media.morepersistentdatatypes.DataType
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.utils.CustomDataTypes
import com.nexomc.nexo.utils.VectorUtils.vectorFromString
import com.nexomc.nexo.utils.safeCast
import com.nexomc.nexo.utils.toFastMap
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import org.joml.Math

data class FurnitureSeat(val offset: Vector) {

    constructor(offset: Map<String?, Any?>) : this(Vector.deserialize(offset))

    constructor(offset: String) : this(vectorFromString(offset, 0f))

    fun offset(): Vector {
        return offset
    }

    /**
     * Offset rotated around the baseEntity's yaw
     *
     * @param yaw Yaw of baseEntity
     * @return Rotated offset vector
     */
    fun offset(yaw: Float): Vector {
        return rotateOffset(yaw)
    }

    private fun rotateOffset(angle: Float): Vector {
        var angle = angle
        if (angle < 0) angle += 360f // Ensure yaw is positive

        val radians = Math.toRadians(angle).toDouble()

        // Get the coordinates relative to the local y-axis
        val x = offset.x * Math.cos(radians) - (-offset.z) * Math.sin(radians)
        val z = offset.x * Math.sin(radians) + (-offset.z) * Math.cos(radians)
        val y = offset.y

        return Vector(x, y, z)
    }

    companion object {
        val SEAT_KEY = NamespacedKey(NexoPlugin.instance(), "seat")

        fun getSeat(offset: Any?) = when (offset) {
            is Map<*, *> -> FurnitureSeat(offset.safeCast<Map<String?, Any?>>()!!)
            is Vector -> FurnitureSeat(offset)
            is String -> FurnitureSeat(offset)
            is Double -> FurnitureSeat(offset.toString())
            is Int -> FurnitureSeat(offset.toString())
            else -> null
        }

        fun getSeats(baseEntity: ItemDisplay, mechanic: FurnitureMechanic): List<Interaction> {
            return baseEntity.takeIf { mechanic.hasSeats }?.persistentDataContainer?.get(SEAT_KEY, CustomDataTypes.UUID_LIST)
                ?.mapNotNull { Bukkit.getEntity(it) as? Interaction } ?: emptyList()
        }

        fun isSeat(entity: Entity?) = entity?.persistentDataContainer?.has(SEAT_KEY, DataType.UUID) == true

        fun sitOnSeat(baseEntity: ItemDisplay, player: Player, interactionPoint: Location?) {
            val centeredLoc = (interactionPoint ?: baseEntity.location).toCenterLocation()
            baseEntity.persistentDataContainer.get(SEAT_KEY, CustomDataTypes.UUID_LIST)
                ?.mapNotNull { Bukkit.getEntity(it)?.takeIf { s -> s.type == EntityType.INTERACTION && s.passengers.isEmpty() } }
                ?.minByOrNull { centeredLoc.distanceSquared(it.location) }
                ?.addPassenger(player)
        }

        fun spawnSeats(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {
            val translation = Vector.fromJOML(baseEntity.transformation.translation)
            val yaw = baseEntity.yaw
            val uuid = baseEntity.uniqueId
            val seatUUIDs = mechanic.seats.map { seat: FurnitureSeat ->
                baseEntity.world.spawn(baseEntity.location.add(translation).add(seat.offset(yaw)), Interaction::class.java) { i ->
                    i.interactionHeight = 0.1f
                    i.interactionWidth = 0.1f
                    i.isPersistent = true
                    i.persistentDataContainer.set(FurnitureMechanic.FURNITURE_KEY, PersistentDataType.STRING, mechanic.itemID)
                    i.persistentDataContainer.set(SEAT_KEY, DataType.UUID, uuid)
                }.uniqueId
            }
            baseEntity.persistentDataContainer.set(SEAT_KEY, CustomDataTypes.UUID_LIST, seatUUIDs)
        }

        fun updateSeats(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {
            val seats = baseEntity.persistentDataContainer.get(SEAT_KEY, CustomDataTypes.UUID_LIST)
                ?.mapIndexedNotNull { i, uuid ->
                    val furnitureSeat = mechanic.seats.elementAtOrNull(i) ?: return@mapIndexedNotNull null
                    val interactionEntity = Bukkit.getEntity(uuid) as? Interaction ?: return@mapIndexedNotNull null
                    furnitureSeat to interactionEntity
                }?.toFastMap() ?: return


            when {
                mechanic.seats.isEmpty -> seats.values.onEach(Entity::remove)
                else -> seats.forEach { (seat, entity) ->
                    val translation = baseEntity.transformation.translation
                    val newLocation = baseEntity.location.add(Vector(translation.x, translation.y, translation.z)).add(seat.offset(baseEntity.yaw))
                    if (newLocation == entity.location) return@forEach

                    val passengers = entity.passengers.toList().onEach(entity::removePassenger)
                    entity.teleportAsync(newLocation).thenRun {
                        passengers.onEach(entity::addPassenger)
                    }
                }
            }
        }

        fun removeSeats(baseEntity: ItemDisplay) {
            baseEntity.persistentDataContainer.getOrDefault(SEAT_KEY, CustomDataTypes.UUID_LIST, listOf())
                .mapNotNull(Bukkit::getEntity).forEach { seat ->
                    seat.passengers.forEach(seat::removePassenger)
                    if (!seat.isDead) seat.remove()
                }
        }
    }
}
