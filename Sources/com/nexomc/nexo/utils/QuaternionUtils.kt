package com.nexomc.nexo.utils

import org.joml.Matrix4f
import org.joml.Quaternionf
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object QuaternionUtils {

    fun fromEuler(x: Float = 0f, y: Float = 0f, z: Float = 0f): Quaternionf {
        val xRad = Math.toRadians(x.toDouble()).toFloat() / 2f
        val yRad = Math.toRadians(y.toDouble()).toFloat() / 2f
        val zRad = Math.toRadians(z.toDouble()).toFloat() / 2f

        val sinX = sin(xRad)
        val cosX = cos(xRad)
        val sinY = sin(yRad)
        val cosY = cos(yRad)
        val sinZ = sin(zRad)
        val cosZ = cos(zRad)

        val xQuat = Quaternionf(sinX, 0f, 0f, cosX)
        val yQuat = Quaternionf(0f, sinY, 0f, cosY)
        val zQuat = Quaternionf(0f, 0f, sinZ, cosZ)

        return zQuat.mul(yQuat, Quaternionf()).mul(xQuat, Quaternionf())
    }

    fun rotationFromYawPitch(yaw: Float, pitch: Float): Quaternionf {
        val yawRad = Math.toRadians(yaw.toDouble()).toFloat()
        val pitchRad = Math.toRadians(pitch.toDouble()).toFloat()

        // Yaw = rotation around Y-axis
        val qYaw = Quaternionf().rotateY(yawRad)
        // Pitch = rotation around X-axis
        val qPitch = Quaternionf().rotateX(pitchRad)

        // Order matters! Apply pitch *after* yaw (Yaw then Pitch = qYaw * qPitch)
        return qYaw.mul(qPitch)
    }

    fun applyYawPitchToMatrix(yaw: Float, pitch: Float): Matrix4f {
        val matrix = Matrix4f()

        val yawRad = Math.toRadians(yaw.toDouble()).toFloat()
        val pitchRad = Math.toRadians(pitch.toDouble()).toFloat()

        // Rotate around Y first (yaw), then X (pitch) — this matches Minecraft's rotation order
        matrix.rotateY(yawRad)
        matrix.rotateX(pitchRad)

        return matrix
    }

    fun leftRotationToYaw(leftRotation: Quaternionf): Float {
        val n = leftRotation.normalize()
        return Math.toDegrees(atan2(2f * (n.y * n.w + n.x * n.z), 1f- 2f * (n.y * n.y + n.x * n.x)).toDouble()).toFloat()
    }

}