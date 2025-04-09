package com.nexomc.nexo.mechanics.furniture.bed

import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import org.bukkit.entity.ItemDisplay

interface IFurnitureBedEntityHandler {
    fun createBedEntity(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, bed: FurnitureBed): IFurnitureBedEntity?
}