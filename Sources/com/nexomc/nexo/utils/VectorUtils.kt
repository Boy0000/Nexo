package com.nexomc.nexo.utils

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.util.Vector
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin

object VectorUtils {

    fun Vector3f.toLocation(world: World) = Location(world, x.toDouble(), y.toDouble(), z.toDouble())

    fun quaternionfFromString(quaternion: String, defaultValue: Float): Quaternionf {
        val floats = quaternion.removeSpaces().split(",").dropLastWhile(String::isEmpty)
            .map { it.toFloatOrNull() ?: defaultValue }.toMutableList()
        while (floats.size < 4) floats += defaultValue
        return Quaternionf(floats[0], floats[1], floats[2], floats[3])
    }

    fun vector3fFromString(vector: String, defaultValue: Float): Vector3f {
        val floats = vector.removeSpaces().split(",").dropLastWhile(String::isEmpty)
            .map { it.toFloatOrNull() ?: defaultValue }.toMutableList()
        while (floats.size < 3) floats.add(defaultValue)
        return Vector3f(floats[0], floats[1], floats[2])
    }

    fun vectorFromString(vector: String, defaultValue: Float): Vector {
        val floats = vector.removeSpaces().split(",").dropLastWhile(String::isEmpty)
            .map { it.toFloatOrNull() ?: defaultValue }.toMutableList()
        while (floats.size < 3) floats.add(defaultValue)
        return Vector(floats[0], floats[1], floats[2])
    }

    fun rotateAroundAxisX(v: Vector, angle: Double) {
        val (cos, sin) = cos(angle) to sin(angle)
        val y = v.y * cos - v.z * sin
        val z = v.y * sin + v.z * cos
        v.setY(y).setZ(z)
    }

    fun rotateAroundAxisY(v: Vector, angle: Double) {
        val (cos, sin) = cos(angle) to sin(angle)
        val x = v.x * cos + v.z * sin
        val z = v.x * -sin + v.z * cos
        v.setX(x).setZ(z)
    }
}
