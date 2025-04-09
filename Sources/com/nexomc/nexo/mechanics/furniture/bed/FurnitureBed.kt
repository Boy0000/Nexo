package com.nexomc.nexo.mechanics.furniture.bed

import com.jeff_media.morepersistentdatatypes.DataType
import com.mineinabyss.idofront.operators.plus
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.nms.NMSHandlers
import com.nexomc.nexo.utils.VectorUtils.vectorFromString
import com.nexomc.nexo.utils.toFastMap
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.math.cos
import kotlin.math.sin

data class FurnitureBed(val offset: Vector, val skipNight: Boolean = true, val resetPhantoms: Boolean = true) {

    constructor(bedString: String) : this(
        vectorFromString(bedString.split(" ").firstOrNull() ?: "", 0f),
        bedString.split(" ").getOrNull(1)?.toBooleanStrictOrNull() ?: true,
        bedString.split(" ").getOrNull(2)?.toBooleanStrictOrNull() ?: true
    )

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

        val radians = Math.toRadians(angle.toDouble())

        // Get the coordinates relative to the local y-axis
        val x = offset.x * cos(radians) - (-offset.z) * sin(radians)
        val z = offset.x * sin(radians) + (-offset.z) * cos(radians)
        val y = offset.y

        return Vector(x, y, z)
    }

    companion object {
        val BED_KEY = NamespacedKey(NexoPlugin.instance(), "bed")

        fun getBeds(baseEntity: ItemDisplay, mechanic: FurnitureMechanic): List<Interaction> {
            return baseEntity.takeIf { mechanic.hasBeds }?.persistentDataContainer?.get(BED_KEY, DataType.asList(DataType.UUID))
                ?.mapNotNull { Bukkit.getEntity(it) as? Interaction } ?: emptyList()
        }

        fun isBed(entity: Entity?) = entity?.persistentDataContainer?.has(BED_KEY, DataType.UUID) == true

        fun sleepOnBed(baseEntity: ItemDisplay, player: Player, interactionPoint: Location?) {
            val centeredLoc = (interactionPoint ?: baseEntity.location).toCenterLocation()
            baseEntity.persistentDataContainer.get(BED_KEY, DataType.asList(DataType.UUID))
                ?.mapNotNull { Bukkit.getEntity(it)?.takeIf { s -> s.type == EntityType.INTERACTION && s.passengers.isEmpty() } }
                ?.minByOrNull { centeredLoc.distanceSquared(it.location) }
                ?.addPassenger(player)
        }

        fun spawnBeds(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {
            val translation = Vector.fromJOML(baseEntity.transformation.translation)
            val yaw = baseEntity.yaw
            val bedUUIDs = mechanic.beds.mapNotNull { bed: FurnitureBed ->
                val spawnLocation = baseEntity.location.add(translation).add(bed.offset(yaw))
                NMSHandlers.handler().bedEntityHandler().createBedEntity(baseEntity, mechanic, bed)?.spawn(spawnLocation)?.uniqueId
            }
            if (bedUUIDs.isNotEmpty()) baseEntity.persistentDataContainer.set(BED_KEY, DataType.asList(DataType.UUID), bedUUIDs)
        }

        fun updateBeds(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {
            val beds = baseEntity.persistentDataContainer.get(BED_KEY, DataType.asList(DataType.UUID))
                ?.mapIndexedNotNull { i, uuid ->
                    val furnitureBed = mechanic.beds.elementAtOrNull(i) ?: return@mapIndexedNotNull null
                    val interactionEntity = Bukkit.getEntity(uuid) as? Interaction ?: return@mapIndexedNotNull null
                    furnitureBed to interactionEntity
                }?.toFastMap() ?: return


            when {
                mechanic.beds.isEmpty() -> beds.values.onEach(Entity::remove)
                else -> beds.forEach { (bed, entity) ->
                    val passengers = entity.passengers.toList().onEach(entity::removePassenger)
                    val translation = Vector.fromJOML(baseEntity.transformation.translation)
                    entity.teleportAsync(baseEntity.location.plus(translation).plus(bed.offset(baseEntity.yaw)))
                    passengers.onEach(entity::addPassenger)
                }
            }
        }

        fun removeBeds(baseEntity: ItemDisplay) {
            baseEntity.persistentDataContainer.getOrDefault(BED_KEY, DataType.asList(DataType.UUID), listOf())
                .mapNotNull(Bukkit::getEntity).forEach { bed ->
                    bed.passengers.forEach(bed::removePassenger)
                    if (!bed.isDead) bed.remove()
                }
        }
    }
}