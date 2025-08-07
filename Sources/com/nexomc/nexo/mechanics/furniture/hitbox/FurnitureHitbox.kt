package com.nexomc.nexo.mechanics.furniture.hitbox

import com.nexomc.nexo.mechanics.furniture.FurnitureFactory
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.utils.flatMapSetFast
import com.nexomc.nexo.utils.getStringListOrNull
import com.nexomc.nexo.utils.mapFast
import com.nexomc.nexo.utils.mapFastSet
import com.nexomc.nexo.utils.plusFast
import com.nexomc.nexo.utils.remove
import com.nexomc.nexo.utils.removeSpaces
import com.nexomc.nexo.utils.toIntRangeOrNull
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.bukkit.Location
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.ItemDisplay
import org.bukkit.util.Vector

data class FurnitureHitbox(
    val barriers: ObjectOpenHashSet<BarrierHitbox> = ObjectOpenHashSet(),
    val interactions: ObjectOpenHashSet<InteractionHitbox> = ObjectOpenHashSet(),
    val shulkers: ObjectOpenHashSet<ShulkerHitbox> = ObjectOpenHashSet(),
    val ghasts: ObjectOpenHashSet<GhastHitbox> = ObjectOpenHashSet(),
) {

    constructor(hitboxSection: ConfigurationSection) : this(
        (hitboxSection.getStringListOrNull("barriers") ?: hitboxSection.getString("barriers")?.let(::listOf))?.flatMapSetFast(::parseHitbox) ?: listOf(),
        hitboxSection.getStringList("interactions").mapFastSet(::InteractionHitbox),
        hitboxSection.getStringList("shulkers").map(::ShulkerHitbox),
        hitboxSection.getStringList("ghasts").map(::GhastHitbox)
    )

    constructor(hitboxList: Collection<String>) : this() {
        hitboxList.forEach { hitbox ->
            val type = hitbox.substringAfterLast(" ")
            val hitbox = hitbox.remove(type)

            when (type) {
                "BARRIER", "B" -> barriers.add(BarrierHitbox(hitbox))
                "INTERACTION", "I" -> interactions.add(InteractionHitbox(hitbox))
                "SHULKER", "S" -> shulkers.add(ShulkerHitbox(hitbox))
                "GHAST", "G" -> ghasts.add(GhastHitbox(hitbox))
            }
        }
    }

    constructor(
        barriers: Collection<BarrierHitbox>,
        interactions: Collection<InteractionHitbox>,
        shulkers: Collection<ShulkerHitbox>,
        ghasts: Collection<GhastHitbox>,
    ) : this(
        ObjectOpenHashSet(barriers), ObjectOpenHashSet(interactions),
        ObjectOpenHashSet(shulkers), ObjectOpenHashSet(ghasts)
    )

    fun hitboxHeight(): Double {
        val highestBarrier = barriers.maxOfOrNull { it.y + 1 } ?: 0
        val highestInteraction = interactions.maxOfOrNull { it.offset.clone().add(Vector(0f, it.height, 0f)).y } ?: 0.0
        val highestShulker = shulkers.maxOfOrNull { it.offset.clone().add(Vector(0f, it.length.toFloat(), 0f)).y } ?: 0.0
        val highestGhast = ghasts.maxOfOrNull { it.offset.clone().add(Vector(0f, it.scale.toFloat(), 0f)).y } ?: 0.0
        return highestInteraction.coerceAtLeast(highestBarrier.toDouble()).coerceAtLeast(highestShulker).coerceAtLeast(highestGhast)
    }

    fun refreshHitboxes(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {
        val packetManager = FurnitureFactory.instance()?.packetManager() ?: return

        packetManager.removeHitboxEntityPacket(baseEntity, mechanic)
        packetManager.removeBarrierHitboxPacket(baseEntity, mechanic)

        packetManager.sendHitboxEntityPacket(baseEntity, mechanic)
        packetManager.sendBarrierHitboxPacket(baseEntity, mechanic)
    }

    fun hitboxLocations(center: Location, yaw: Float) = barrierLocations(center, yaw)
        .plusFast(interactionLocations(center, yaw))
        .plusFast(shulkerLocations(center, yaw))

    fun barrierLocations(center: Location, rotation: Float) =
        barriers.mapFast { it.groundRotate(rotation).add(center) }

    fun interactionLocations(center: Location, rotation: Float) =
        interactions.mapFast { center.toCenterLocation().add(it.offset(rotation)) }

    fun interactionBoundingBoxes(center: Location, rotation: Float) =
        interactions.mapFast { it.boundingBox(center.toCenterLocation().add(it.offset(rotation))) }

    fun shulkerLocations(center: Location, rotation: Float) =
        shulkers.mapFast { center.clone().add(it.offset(rotation)) }

    companion object {
        val EMPTY = FurnitureHitbox()

        fun parseHitbox(hitboxString: String): List<BarrierHitbox> {
            return when {
                hitboxString == "origin" -> listOf(BarrierHitbox("0,0,0"))
                ".." in hitboxString -> {
                    // Split the coordinates by commas
                    val coordinates = hitboxString.split(",", limit = 3).map { r -> r.removeSpaces().toIntRangeOrNull() ?: (r.toIntOrNull() ?: 0).let { IntRange(it, it) } }

                    val (xRange, yRange, zRange) = coordinates
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
