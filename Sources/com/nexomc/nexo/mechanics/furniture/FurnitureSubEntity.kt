package com.nexomc.nexo.mechanics.furniture

import com.nexomc.nexo.api.NexoFurniture
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import org.bukkit.Bukkit
import org.bukkit.entity.ItemDisplay
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import java.util.*

class FurnitureSubEntity {
    private val mechanic: FurnitureMechanic
    val baseUuid: UUID
    val baseId: Int
    val entityIds: IntArrayList
    val boundingBoxes: ObjectArrayList<BoundingBox>

    constructor(mechanic: FurnitureMechanic, baseEntity: ItemDisplay, entityIds: Collection<Int>, boundingBoxes: List<BoundingBox>) {
        this.mechanic = mechanic
        this.baseUuid = baseEntity.uniqueId
        this.baseId = baseEntity.entityId
        this.entityIds = entityIds as? IntArrayList ?: IntArrayList(entityIds)
        this.boundingBoxes = boundingBoxes as? ObjectArrayList ?: ObjectArrayList(boundingBoxes)
    }

    fun baseEntity(): ItemDisplay? {
        return Bukkit.getEntity(baseUuid) as? ItemDisplay
    }

    fun hitboxLocation(entityId: Int): Vector? {
        return runCatching {
            boundingBoxes.getOrNull(entityIds.indexOf(entityId))
        }.getOrNull()?.center
    }

    fun mechanic() = NexoFurniture.furnitureMechanic(mechanic.section.name) ?: mechanic
}
