package com.nexomc.nexo.mechanics.furniture.bed

import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.mechanics.trident.ICustomTridentEntity
import com.nexomc.nexo.mechanics.trident.TridentMechanic
import com.nexomc.nexo.nms.ICustomEntityHandler
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Trident

class EmptyCustomEntityHandler : ICustomEntityHandler {
    override fun createTridentEntity(tridentProjectile: Trident, mechanic: TridentMechanic): ICustomTridentEntity? {
        return null
    }
    override fun createBedEntity(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, bed: FurnitureBed): IFurnitureBedEntity? {
        return null
    }
}