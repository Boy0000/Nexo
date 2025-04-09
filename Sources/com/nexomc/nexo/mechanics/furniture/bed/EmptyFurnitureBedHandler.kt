package com.nexomc.nexo.mechanics.furniture.bed

import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import org.bukkit.entity.ItemDisplay

class EmptyFurnitureBedHandler : IFurnitureBedEntityHandler {
    override fun createBedEntity(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, bed: FurnitureBed): IFurnitureBedEntity? {
        return null
    }
}