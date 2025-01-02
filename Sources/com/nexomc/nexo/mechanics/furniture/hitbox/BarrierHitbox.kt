package com.nexomc.nexo.mechanics.furniture.hitbox

import com.nexomc.nexo.mechanics.furniture.BlockLocation
import org.bukkit.Location
import org.joml.Vector3f

class BarrierHitbox : BlockLocation {
    fun from(hitboxObject: Any?): BarrierHitbox {
        return when (hitboxObject) {
            is String -> BarrierHitbox(hitboxObject)

            else -> BarrierHitbox("0,0,0")
        }
    }

    constructor(x: Int, y: Int, z: Int) : super(x, y, z)

    constructor(hitboxString: String) : super(hitboxString)

    constructor(location: Location) : super(location)
}
