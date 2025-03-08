package com.nexomc.nexo.mechanics.furniture

import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntList
import org.bukkit.Bukkit
import org.bukkit.entity.ItemDisplay
import java.util.*
import org.bukkit.util.BoundingBox

class FurnitureSubEntity {
    val baseUuid: UUID
    val baseId: Int
    val entityIds: IntList
    val boundingBoxes: List<BoundingBox>

    constructor(baseEntity: ItemDisplay, entityIds: Collection<Int>, boundingBoxes: List<BoundingBox>) {
        this.baseUuid = baseEntity.uniqueId
        this.baseId = baseEntity.entityId
        this.entityIds = IntArrayList(entityIds)
        this.boundingBoxes = ArrayList(boundingBoxes)
    }

    constructor(baseUuid: UUID, baseId: Int, entityIds: Collection<Int>, boundingBoxes: List<BoundingBox>) {
        this.baseUuid = baseUuid
        this.baseId = baseId
        this.entityIds = IntArrayList(entityIds)
        this.boundingBoxes = boundingBoxes
    }

    fun equalsBase(baseEntity: ItemDisplay): Boolean {
        return baseUuid == baseEntity.uniqueId && baseId == baseEntity.entityId
    }

    fun baseEntity(): ItemDisplay? {
        return Bukkit.getEntity(baseUuid) as ItemDisplay?
    }
}
