package com.nexomc.nexo.mechanics.furniture.hitbox

import com.nexomc.nexo.mechanics.furniture.BlockLocation
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.data.Waterlogged

class BarrierHitbox : BlockLocation {
    val barrierData: Waterlogged

    fun from(hitboxObject: Any?): BarrierHitbox {
        return when (hitboxObject) {
            is String -> BarrierHitbox(hitboxObject)

            else -> BarrierHitbox("0,0,0")
        }
    }

    constructor(x: Int, y: Int, z: Int) : super(x, y, z) {
        barrierData = IFurniturePacketManager.BARRIER_DATA
    }

    constructor(hitboxString: String) : super(hitboxString) {
        barrierData = IFurniturePacketManager.BARRIER_DATA
    }

    constructor(location: Location) : super(location) {
        barrierData = IFurniturePacketManager.BARRIER_DATA.takeUnless {
            location.block.type == Material.WATER
        } ?: IFurniturePacketManager.BARRIER_DATA_WATERLOGGED
    }
}
