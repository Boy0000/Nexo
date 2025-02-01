package com.nexomc.nexo.mechanics.furniture.hitbox

import com.nexomc.nexo.utils.VectorUtils.vectorFromString
import org.bukkit.block.BlockFace
import org.bukkit.util.Vector
import org.joml.Math
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin

data class ShulkerHitbox(
    val offset: Vector = Vector(),
    val scale: Double = 1.0,
    val length: Double = 1.0,
    val direction: BlockFace = BlockFace.DOWN,
) {

    constructor(hitboxString: String) : this(
        offset = vectorFromString(hitboxString.split(" ").firstOrNull() ?: "0,0,0", 0f),
        scale = hitboxString.split(" ").getOrElse(1) { "1.0" }.toDoubleOrNull() ?: 1.0,
        length = hitboxString.split(" ").getOrElse(2) { "0.0" }.toDoubleOrNull()?.coerceIn(1.0..2.0) ?: 1.0,
        direction = BlockFace.entries.firstOrNull { it.name.uppercase() == hitboxString.split(" ").getOrElse(3) { "DOWN" }.uppercase() } ?: BlockFace.DOWN,
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

    fun direction(angle: Float): Vector3f {
        var angle = angle
        if (angle < 0) angle += 360f

        // Convert BlockFace to a direction vector
        val dirVector = when (direction) {
            BlockFace.NORTH -> Vector(0, 0, -1)
            BlockFace.SOUTH -> Vector(0, 0, 1)
            BlockFace.WEST -> Vector(-1, 0, 0)
            BlockFace.EAST -> Vector(1, 0, 0)
            else -> return direction.direction.toVector3f() // Return unchanged if it's UP/DOWN
        }

        val radians = Math.toRadians(angle.toDouble())

        // Apply 2D rotation around the Y-axis
        val x = dirVector.x * cos(radians) - (-dirVector.z) * sin(radians)
        val z = dirVector.x * sin(radians) + (-dirVector.z) * cos(radians)

        val rotatedVector = Vector(x, 0.0, z)

        // Map to closest BlockFace
        return (listOf(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST)
            .minByOrNull { it.direction.distance(rotatedVector) } ?: direction).direction.toVector3f()
    }


    companion object {
        var DEFAULT = ShulkerHitbox()
    }
}