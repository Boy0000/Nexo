package com.nexomc.nexo.mechanics.furniture.hitbox

import com.nexomc.nexo.mechanics.furniture.FurnitureFactory
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager
import com.nexomc.nexo.utils.toIntRangeOrNull
import org.bukkit.Location
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.ItemDisplay
import org.bukkit.util.Vector

class FurnitureHitbox(
    private val barriers: Set<BarrierHitbox> = setOf(),
    private val interactions: Set<InteractionHitbox> = setOf(),
) {

    constructor(hitboxSection: ConfigurationSection) : this(
        hitboxSection.getStringList("barriers").map(::parseHitbox).flatten().toSet(),
        hitboxSection.getStringList("interactions").map(::InteractionHitbox)
    )

    constructor(barriers: Collection<BarrierHitbox>, interactions: Collection<InteractionHitbox>)
        : this(barriers.toSet(), interactions.toSet())

    fun barriers(): Set<BarrierHitbox> {
        return barriers
    }

    fun interactions(): List<InteractionHitbox> {
        return interactions.toList()
    }

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
        barrierLocations(center, yaw).plus(interactionLocations(center, yaw))

    fun barrierLocations(center: Location, rotation: Float) =
        barriers.map { it.groundRotate(rotation).add(center) }

    fun interactionLocations(center: Location, rotation: Float) =
        interactions.map { center.toCenterLocation().clone().add(it.offset(rotation)) }

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
