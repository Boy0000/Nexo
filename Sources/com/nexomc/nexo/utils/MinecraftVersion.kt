/*
 *  ProtocolLib - Bukkit server library that allows access to the Minecraft protocol.
 *  Copyright (C) 2012 Kristian S. Stangeland
 *
 *  This program is free software; you can redistribute it and/or modify it under the terms of the
 *  GNU General Public License as published by the Free Software Foundation; either version 2 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with this program;
 *  if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307 USA
 */
package com.nexomc.nexo.utils

import com.google.common.collect.ComparisonChain
import io.papermc.paper.ServerBuildInfo
import org.bukkit.Bukkit
import java.io.Serializable
import java.util.*
import kotlin.concurrent.Volatile
import kotlin.math.min

/**
 * Determine the current Minecraft version.
 *
 * @author Kristian
 */
class MinecraftVersion : Comparable<MinecraftVersion?>, Serializable {
    /**
     * Major version number
     *
     * @return Current major version number.
     */
    val major: Int

    /**
     * Minor version number
     *
     * @return Current minor version number.
     */
    val minor: Int

    /**
     * Build version number
     *
     * @return Current build version number.
     */
    val build: Int

    @Volatile
    private var atCurrentOrAbove: Boolean? = null

    /**
     * Construct a version format from the standard release version or the snapshot verison.
     *
     * @param versionOnly   - the version.
     */
    constructor(versionOnly: String) {
        val section = versionOnly.split("-").dropLastWhile { it.isEmpty() }.toTypedArray()
        val numbers = this.parseVersion(section[0])

        this.major = numbers[0]
        this.minor = numbers[1]
        this.build = numbers[2]
    }

    private fun parseVersion(version: String): IntArray {
        val elements = version.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val numbers = IntArray(3)

        // Make sure it's even a valid version
        check(elements.isNotEmpty()) { "Corrupt MC version: $version" }

        // The String 1 or 1.2 is interpreted as 1.0.0 and 1.2.0 respectively.
        for (i in 0..<min(numbers.size.toDouble(), elements.size.toDouble()).toInt()) {
            numbers[i] = elements[i].trim { it <= ' ' }.toInt()
        }
        return numbers
    }

    /**
     * Checks if this version is at or above the current version the server is running.
     *
     * @return true if this version is equal or newer than the server version, false otherwise.
     */
    fun atOrAbove(): Boolean {
        if (this.atCurrentOrAbove == null) this.atCurrentOrAbove = atOrAbove(this)

        return atCurrentOrAbove!!
    }

    val version: String
        get() = "$major.$minor.$build"

    override fun compareTo(other: MinecraftVersion?): Int {
        if (other == null) return 1

        return ComparisonChain.start()
            .compare(this.major, other.major)
            .compare(this.minor, other.minor)
            .compare(this.build, other.build)
            .result()
    }

    fun isAtLeast(other: MinecraftVersion?): Boolean {
        if (other == null) return false
        return this >= other
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true

        return when (other) {
            is MinecraftVersion -> this.major == other.major && this.minor == other.minor && this.build == other.build
            else -> false
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(this.major, this.minor, this.build)
    }

    override fun toString(): String {
        return String.format("(MC: %s)", this.version)
    }

    companion object {

        private val sharedConstants = runCatching { Class.forName("net.minecraft.SharedConstants") }.getOrNull()
        private val detectedVersionClazz = runCatching { Class.forName("net.minecraft.DetectedVersion") }.getOrNull()

        @JvmStatic
        val currentVersion by lazy {
            MinecraftVersion(runCatching {
                ServerBuildInfo.buildInfo().minecraftVersionName()
            }.getOrNull() ?: Bukkit.getMinecraftVersion())
        }

        private fun atOrAbove(version: MinecraftVersion): Boolean {
            return currentVersion.isAtLeast(version)
        }
    }
}