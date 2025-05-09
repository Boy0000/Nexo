package com.nexomc.nexo.utils

import org.bukkit.Location
import org.bukkit.entity.Entity

fun Location.plus(x: Int = 0, y: Int = 0, z: Int = 0): Location {
    return Location(this.world, this.x + x, this.y + y, this.z + z)
}

fun Location.plus(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0): Location {
    return Location(this.world, this.x + x, this.y + y, this.z + z)
}

fun Location.minus(x: Int = 0, y: Int = 0, z: Int = 0): Location {
    return Location(this.world, this.x - x, this.y - y, this.z - z)
}

fun Location.minus(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0): Location {
    return Location(this.world, this.x - x, this.y - y, this.z - z)
}

val Entity.blockLocation get() = this.location.toBlockLocation()

object LocationUtils {
}