package com.nexomc.nexo.mechanics.furniture.hitbox

import com.nexomc.nexo.utils.VectorUtils.vectorFromString
import org.apache.commons.lang3.StringUtils
import org.bukkit.util.Vector
import org.joml.Math
import kotlin.math.cos
import kotlin.math.sin

data class InteractionHitbox(
    val offset: Vector = Vector(),
    val width: Float = 1f,
    val height: Float = 1f
) {

    // Constructor taking a hitboxObject
    constructor(hitboxObject: Any?) : this(
        (hitboxObject as? String)?.let(::InteractionHitbox)?.offset ?: DEFAULT.offset,
        (hitboxObject as? String)?.let(::InteractionHitbox)?.width ?: DEFAULT.width,
        (hitboxObject as? String)?.let(::InteractionHitbox)?.height ?: DEFAULT.height
    )

    // Constructor taking a hitboxString
    constructor(hitboxString: String) : this(
        offset = vectorFromString(StringUtils.split(hitboxString, " ", 2).firstOrNull() ?: "0,0,0", 0f),
        width = StringUtils.split(hitboxString, " ", 2).getOrNull(1)?.substringBefore(",")?.toFloatOrNull() ?: 1f,
        height = StringUtils.split(hitboxString, " ", 2).getOrNull(1)?.substringAfter(",")?.toFloatOrNull() ?: 1f
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

    companion object {
        var DEFAULT = InteractionHitbox()
    }
}
