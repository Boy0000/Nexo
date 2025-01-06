package com.nexomc.nexo.mechanics.furniture.hitbox

import com.nexomc.nexo.mechanics.furniture.FurnitureFactory
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager
import com.nexomc.nexo.utils.*
import com.nexomc.nexo.utils.toIntRangeOrNull
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.bukkit.Location
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.ItemDisplay
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import kotlin.collections.plus

class FurnitureHitbox(
    val barriers: ObjectOpenHashSet<BarrierHitbox> = ObjectOpenHashSet(),
    val interactions: ObjectOpenHashSet<InteractionHitbox> = ObjectOpenHashSet(),
) {

    constructor(hitboxSection: ConfigurationSection) : this(
        hitboxSection.getStringList("barriers").flatMapSetFast(::parseHitbox),
        hitboxSection.getStringList("interactions").mapFastSet(::InteractionHitbox)
    )

    constructor(barriers: Collection<BarrierHitbox>, interactions: Collection<InteractionHitbox>)
        : this(ObjectOpenHashSet(barriers), ObjectOpenHashSet(interactions))

    fun hitboxHeight(): Double {
        val highestBarrier = barriers.maxOfOrNull { it.y + 1 } ?: 0
        val highestInteraction = interactions.maxOfOrNull { it.offset.clone().add(Vector(0f, it.height, 0f)).y } ?: 0.0
        return highestInteraction.coerceAtLeast(highestBarrier.toDouble())
    }

    fun refreshHitboxes(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {
        val packetManager = FurnitureFactory.instance()?.packetManager() ?: return

        packetManager.removeInteractionHitboxPacket(baseEntity, mechanic)
        packetManager.removeBarrierHitboxPacket(baseEntity, mechanic)
        packetManager.removeLightMechanicPacket(baseEntity, mechanic)

        packetManager.sendInteractionEntityPacket(baseEntity, mechanic)
        packetManager.sendBarrierHitboxPacket(baseEntity, mechanic)
        packetManager.sendLightMechanicPacket(baseEntity, mechanic)
    }

    fun hitboxLocations(center: Location, yaw: Float) =
        barrierLocations(center, yaw).plusFast(interactionLocations(center, yaw))

    fun barrierLocations(center: Location, rotation: Float) =
        barriers.mapFast { it.groundRotate(rotation).add(center) }

    fun interactionLocations(center: Location, rotation: Float) =
        interactions.mapFast { center.toCenterLocation().clone().add(it.offset(rotation)) }

    fun interactionBoundingBoxes(center: Location, rotation: Float) =
        interactions.mapFast { it.boundingBox(center.toCenterLocation().clone().add(it.offset(rotation))) }

    companion object {
        val EMPTY = FurnitureHitbox()

        fun parseHitbox(hitboxString: String): List<BarrierHitbox> {
            return when {
                hitboxString == "origin" -> listOf(BarrierHitbox("0,0,0"))
                ".." in hitboxString -> {
                    // Split the coordinates by commas
                    val coordinates = hitboxString.split(",").map { r -> r.toIntRangeOrNull() ?: (r.toIntOrNull() ?: 0).let { IntRange(it, it) } }

                    val xRange = coordinates[0]
                    val yRange = coordinates[1]
                    val zRange = coordinates[2]

                    // Generate combinations of all the ranges
                    mutableListOf<BarrierHitbox>().apply {
                        for (x in xRange) for (y in yRange) for (z in zRange) {
                            this += BarrierHitbox(x, y, z)
                        }
                    }
                }
                else -> listOf(BarrierHitbox(hitboxString))
            }
        }
    }
}
