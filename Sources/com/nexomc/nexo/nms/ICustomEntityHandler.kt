package com.nexomc.nexo.nms

import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.mechanics.furniture.bed.FurnitureBed
import com.nexomc.nexo.mechanics.furniture.bed.IFurnitureBedEntity
import com.nexomc.nexo.mechanics.trident.ICustomTridentEntity
import com.nexomc.nexo.mechanics.trident.TridentMechanic
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Trident

interface ICustomEntityHandler {
    fun createTridentEntity(tridentProjectile: Trident, mechanic: TridentMechanic): ICustomTridentEntity?
    fun createBedEntity(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, bed: FurnitureBed): IFurnitureBedEntity?
}