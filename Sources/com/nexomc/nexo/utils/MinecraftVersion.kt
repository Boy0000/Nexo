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
import com.google.common.collect.Ordering
import org.bukkit.Bukkit
import org.bukkit.Server
import java.io.Serializable
import java.util.*
import java.util.regex.Pattern
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
    /**
     * Retrieve the development stage.
     *
     * @return Development stage, or NULL if this is a release.
     */
    // The development stage

    val developmentStage: String?

    @Volatile
    private var atCurrentOrAbove: Boolean? = null

    /**
     * Determine the current Minecraft version.
     *
     * @param server - the Bukkit server that will be used to examine the MC version.
     */
    constructor(server: Server) : this(extractVersion(server.version))

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
        this.developmentStage = section.getOrNull(1)
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
        /**
         * Retrieve the version String (major.minor.build) only.
         *
         * @return A normal version string.
         */
        get() {
            return if (this.developmentStage == null) {
                String.format(
                    "%s.%s.%s",
                    major,
                    minor,
                    build
                )
            } else {
                String.format(
                    "%s.%s.%s-%s%s",
                    major,
                    minor,
                    build,
                    developmentStage,
                    ""
                )
            }
        }

    override fun compareTo(o: MinecraftVersion?): Int {
        if (o == null) {
            return 1
        }

        return ComparisonChain.start()
            .compare(this.major, o.major)
            .compare(this.minor, o.minor)
            .compare(this.build, o.build)
            .compare(
                this.developmentStage,
                o.developmentStage, Ordering.natural<Comparable<*>>().nullsLast()
            ).result()
    }

    fun isAtLeast(other: MinecraftVersion?): Boolean {
        if (other == null) return false
        return this >= other
    }

    override fun equals(obj: Any?): Boolean {
        if (obj == null) return false
        if (obj === this) return true

        if (obj is MinecraftVersion) {
            val other = obj

            return this.major == other.major && this.minor == other.minor && this.build == other.build &&
                    this.developmentStage == other.developmentStage
        }

        return false
    }

    override fun hashCode(): Int {
        return Objects.hash(this.major, this.minor, this.build)
    }

    override fun toString(): String {
        // Convert to a String that we can parse back again
        return String.format("(MC: %s)", this.version)
    }

    companion object {

        private val VERSION_PATTERN = Pattern.compile(".*\\(.*MC.\\s*([a-zA-z0-9\\-.]+).*")

        @JvmStatic
        val currentVersion by lazy { fromServerVersion(Bukkit.getVersion()) }

        /**
         * Extract the Minecraft version from CraftBukkit itself.
         *
         * @param text - the server version in text form.
         * @return The underlying MC version.
         * @throws IllegalStateException If we could not parse the version string.
         */
        fun extractVersion(text: String): String {
            val version = VERSION_PATTERN.matcher(text)

            if (version.matches() && version.group(1) != null) {
                return version.group(1)
            } else {
                throw IllegalStateException("Cannot parse version String '$text'")
            }
        }

        /**
         * Parse the given server version into a Minecraft version.
         *
         * @param serverVersion - the server version.
         * @return The resulting Minecraft version.
         */
        fun fromServerVersion(serverVersion: String): MinecraftVersion {
            return MinecraftVersion(extractVersion(serverVersion))
        }

        private fun atOrAbove(version: MinecraftVersion): Boolean {
            return currentVersion.isAtLeast(version)
        }
    }
}