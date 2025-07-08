package com.nexomc.nexo.mechanics.furniture.hitbox

import com.nexomc.nexo.utils.VectorUtils.vectorFromString
import org.bukkit.Location
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import org.joml.Math
import kotlin.math.cos
import kotlin.math.sin

data class GhastHitbox(val offset: Vector = Vector(), val scale: Double = 1.0, val rotation: Float = 0f, val visible: Boolean = false) {

    constructor(hitboxString: String) : this(
        offset = vectorFromString(hitboxString.split(" ").firstOrNull() ?: "0,0,0", 0f),
        scale = hitboxString.split(" ").getOrNull(1)?.toDoubleOrNull() ?: 0.25,
        rotation = hitboxString.split(" ").getOrNull(2)?.toFloatOrNull() ?: 0f,
        visible = hitboxString.substringAfterLast(" ").toBooleanStrictOrNull() ?: false
    )

    /**
     * Offset rotated around the baseEntity's yaw
     * @param angle Yaw of baseEntity
     * @return Rotated offset vector
     */
    fun offset(angle: Float): Vector {
        var angle = angle
        if (angle < 0) angle += 360f

        val radians = Math.toRadians(angle).toDouble()
        val x = offset.x * cos(radians) - (-offset.z) * sin(radians)
        val z = offset.x * sin(radians) + (-offset.z) * cos(radians)

        return Vector(x, offset.y, z)
    }

    fun boundingBox(center: Location): BoundingBox {
        return BoundingBox.of(center, scale.div(2.0), scale.div(2.0), scale.div(2.0))
    }

    companion object {
        var DEFAULT = GhastHitbox()
    }
}