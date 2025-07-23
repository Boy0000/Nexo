package com.nexomc.nexo.mechanics.furniture

import com.jeff_media.morepersistentdatatypes.datatypes.serializable.ConfigurationSerializableDataType
import com.nexomc.nexo.utils.removeSpaces
import com.nexomc.nexo.utils.to
import org.bukkit.Location
import org.bukkit.Utility
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.util.NumberConversions
import org.bukkit.util.Vector
import org.joml.Math
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

open class BlockLocation(var x: Int = 0, var y: Int = 0, var z: Int = 0) : ConfigurationSerializable {

    constructor(location: Location) : this(location.blockX, location.blockY, location.blockZ)

    constructor(block: Block) : this(block.x, block.y, block.z)

    constructor(location: String) : this() {
        val split = location.split(",").map { it.removeSpaces().toIntOrNull() ?: 0 }.toMutableList()
        while (split.size < 3) split += 0

        x = split[0]
        y = split[1]
        z = split[2]
    }

    override fun toString() = "$x,$y,$z"

    fun toVector3f() = Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
    fun toVector() = Vector(x, y, z)

    fun distanceTo(other: BlockLocation): Double {
        val (x, y, z) = x.toDouble() to y.toDouble() to z.toDouble()
        return NumberConversions.square(x - other.x) + NumberConversions.square(y - other.y) + NumberConversions.square(z - other.z)
    }

    fun add(blockLocation: BlockLocation): BlockLocation {
        this.x += blockLocation.x
        this.y += blockLocation.y
        this.z += blockLocation.z
        return this
    }

    fun add(x: Int, y: Int, z: Int): BlockLocation {
        this.x += x
        this.y += y
        this.z += z
        return this
    }

    fun add(location: Location) = location.clone().add(x.toDouble(), y.toDouble(), z.toDouble())

    fun subtract(blockLocation: BlockLocation): BlockLocation {
        this.x -= blockLocation.x
        this.y -= blockLocation.y
        this.z -= blockLocation.z
        return this
    }

    fun subtract(x: Int, y: Int, z: Int): BlockLocation {
        this.x -= x
        this.y -= y
        this.z -= z
        return this
    }

    fun subtract(location: Location) = location.clone().subtract(x.toDouble(), y.toDouble(), z.toDouble())

    fun toLocation(world: World) = Location(world, x.toDouble(), y.toDouble(), z.toDouble())

    fun groundRotate(angle: Float): BlockLocation {
        var angle = angle
        if (angle < 0) angle += 360f // Ensure angle is positive

        val radians = Math.toRadians(angle).toDouble()

        // Standard 2D rotation matrix for counterclockwise rotation
        val newX = round(x * cos(radians) - (-z) * sin(radians)).toInt()
        val newZ = round(x * sin(radians) + (-z) * cos(radians)).toInt()

        return BlockLocation(newX, y, newZ)
    }

    override fun equals(obj: Any?): Boolean {
        return obj is BlockLocation && obj.x == x && obj.y == y && obj.z == z
    }

    @Utility
    override fun serialize() = mutableMapOf(
        "x" to x,
        "y" to y,
        "z" to z,
    )

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    companion object {
        var dataType = ConfigurationSerializableDataType(BlockLocation::class.java)

        var ZERO = BlockLocation()

        fun deserialize(args: Map<String?, Any?>) = BlockLocation(args["x"] as Int, args["y"] as Int, args["z"] as Int)
    }
}
