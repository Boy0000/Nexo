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
import java.text.SimpleDateFormat
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

    /**
     * Retrieve the snapshot version, or NULL if this is a release.
     *
     * @return The snapshot version.
     */
    // Snapshot?
    val snapshot: SnapshotVersion?

    @Volatile
    private var atCurrentOrAbove: Boolean? = null

    /**
     * Determine the current Minecraft version.
     *
     * @param server - the Bukkit server that will be used to examine the MC version.
     */
    constructor(server: Server) : this(extractVersion(server.version))

    /**
     * Construct a version object from the format major.minor.build, or the snapshot format.
     *
     * @param versionOnly - the version in text form.
     */
    constructor(versionOnly: String) : this(versionOnly, true)

    /**
     * Construct a version format from the standard release version or the snapshot verison.
     *
     * @param versionOnly   - the version.
     * @param parseSnapshot - TRUE to parse the snapshot, FALSE otherwise.
     */
    private constructor(versionOnly: String, parseSnapshot: Boolean) {
        val section = versionOnly.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var snapshot: SnapshotVersion? = null
        var numbers = IntArray(3)

        try {
            numbers = this.parseVersion(section[0])
        } catch (cause: NumberFormatException) {
            // Skip snapshot parsing
            if (!parseSnapshot) {
                throw cause
            }

            try {
                // Determine if the snapshot is newer than the current release version
                snapshot = SnapshotVersion(section[0])
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)

                val latest = TRICKY_TRIALS
                val newer = false

                numbers[0] = latest.major
                numbers[1] = latest.minor + (if (newer) 1 else -1)
            } catch (e: Exception) {
                throw IllegalStateException("Cannot parse " + section[0], e)
            }
        }

        this.major = numbers[0]
        this.minor = numbers[1]
        this.build = numbers[2]
        this.developmentStage = if (section.size > 1) section[1] else (if (snapshot != null) "snapshot" else null)
        this.snapshot = snapshot
    }

    /**
     * Construct a version object directly.
     *
     * @param major       - major version number.
     * @param minor       - minor version number.
     * @param build       - build version number.
     * @param development - development stage.
     */
    /**
     * Construct a version object directly.
     *
     * @param major - major version number.
     * @param minor - minor version number.
     * @param build - build version number.
     */
    @JvmOverloads
    constructor(major: Int, minor: Int, build: Int, development: String? = null) {
        this.major = major
        this.minor = minor
        this.build = build
        this.developmentStage = development
        this.snapshot = null
    }

    private fun parseVersion(version: String): IntArray {
        val elements = version.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val numbers = IntArray(3)

        // Make sure it's even a valid version
        check(elements.size >= 1) { "Corrupt MC version: $version" }

        // The String 1 or 1.2 is interpreted as 1.0.0 and 1.2.0 respectively.
        for (i in 0..<min(numbers.size.toDouble(), elements.size.toDouble()).toInt()) {
            numbers[i] = elements[i].trim { it <= ' ' }.toInt()
        }
        return numbers
    }

    /**
     * Determine if this version is a snapshot.
     *
     * @return The snapshot version.
     */
    fun isSnapshot(): Boolean {
        return this.snapshot != null
    }

    /**
     * Checks if this version is at or above the current version the server is running.
     *
     * @return true if this version is equal or newer than the server version, false otherwise.
     */
    fun atOrAbove(): Boolean {
        if (this.atCurrentOrAbove == null) {
            this.atCurrentOrAbove = atOrAbove(this)
        }

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
                    if (this.isSnapshot()) snapshot else ""
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
            )
            .compare(
                this.snapshot,
                o.snapshot, Ordering.natural<Comparable<*>>().nullsFirst()
            )
            .result()
    }

    fun isAtLeast(other: MinecraftVersion?): Boolean {
        if (other == null) {
            return false
        }

        return this.compareTo(other) >= 0
    }

    override fun equals(obj: Any?): Boolean {
        if (obj == null) {
            return false
        }
        if (obj === this) {
            return true
        }

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
        val v1_21_2 = MinecraftVersion("1.21.3")
        val v1_21_1 = MinecraftVersion("1.21.1")
        val TRICKY_TRIALS = MinecraftVersion("1.21")

        /**
         * Version 1.20.5 - the cookie and transfer packet update
         */
        val v1_20_5 = MinecraftVersion("1.20.5")

        /**
         * Version 1.20.4 - the decorated pot update
         */
        val v1_20_4 = MinecraftVersion("1.20.4")

        /**
         * Version 1.20.2 - the update that added the configuration protocol phase.
         */
        val CONFIG_PHASE_PROTOCOL_UPDATE = MinecraftVersion("1.20.2")

        /**
         * Version 1.20 - the trails and tails update
         */
        val TRAILS_AND_TAILS = MinecraftVersion("1.20")

        /**
         * Version 1.19.4 - the rest of the feature preview
         */
        val FEATURE_PREVIEW_2 = MinecraftVersion("1.19.4")

        /**
         * Version 1.19.3 - introducing feature preview
         */
        val FEATURE_PREVIEW_UPDATE = MinecraftVersion("1.19.3")

        /**
         * Version 1.19 - the wild update
         */
        val WILD_UPDATE = MinecraftVersion("1.19")

        /**
         * Version 1.18 - caves and cliffs part 2
         */
        val CAVES_CLIFFS_2 = MinecraftVersion("1.18")

        /**
         * Version 1.17 - caves and cliffs part 1
         */
        val CAVES_CLIFFS_1 = MinecraftVersion("1.17")

        /**
         * Version 1.16.4
         */
        val NETHER_UPDATE_4 = MinecraftVersion("1.16.4")

        /**
         * Version 1.16.2 - breaking change to the nether update
         */
        val NETHER_UPDATE_2 = MinecraftVersion("1.16.2")

        /**
         * Version 1.16.0 - the nether update
         */
        val NETHER_UPDATE = MinecraftVersion("1.16")

        /**
         * Version 1.15 - the bee update
         */
        val BEE_UPDATE = MinecraftVersion("1.15")

        /**
         * Version 1.14 - village and pillage update.
         */
        val VILLAGE_UPDATE = MinecraftVersion("1.14")

        /**
         * Version 1.13 - update aquatic.
         */
        val AQUATIC_UPDATE = MinecraftVersion("1.13")

        /**
         * Version 1.12 - the world of color update.
         */
        val COLOR_UPDATE = MinecraftVersion("1.12")

        /**
         * Version 1.11 - the exploration update.
         */
        val EXPLORATION_UPDATE = MinecraftVersion("1.11")

        /**
         * Version 1.10 - the frostburn update.
         */
        val FROSTBURN_UPDATE = MinecraftVersion("1.10")

        /**
         * Version 1.9 - the combat update.
         */
        val COMBAT_UPDATE = MinecraftVersion("1.9")

        /**
         * Version 1.8 - the "bountiful" update.
         */
        val BOUNTIFUL_UPDATE = MinecraftVersion("1.8")

        /**
         * Version 1.7.8 - the update that changed the skin format (and distribution - R.I.P. player disguise)
         */
        val SKIN_UPDATE = MinecraftVersion("1.7.8")

        /**
         * Version 1.7.2 - the update that changed the world.
         */
        val WORLD_UPDATE = MinecraftVersion("1.7.2")

        /**
         * Version 1.6.1 - the horse update.
         */
        val HORSE_UPDATE = MinecraftVersion("1.6.1")

        /**
         * Version 1.5.0 - the redstone update.
         */
        val REDSTONE_UPDATE = MinecraftVersion("1.5.0")

        /**
         * Version 1.4.2 - the scary update (Wither Boss).
         */
        val SCARY_UPDATE = MinecraftVersion("1.4.2")

        /**
         * The latest release version of minecraft.
         */
        val LATEST = v1_20_5

        // used when serializing
        private const val serialVersionUID = -8695133558996459770L

        /**
         * Regular expression used to parse version strings.
         */
        private val VERSION_PATTERN = Pattern.compile(".*\\(.*MC.\\s*([a-zA-z0-9\\-.]+).*")

        /**
         * The current version of minecraft, lazy initialized by MinecraftVersion.currentVersion()
         */
        @JvmStatic
        var currentVersion: MinecraftVersion? = null
            get() {
                if (field == null) {
                    field =
                        fromServerVersion(Bukkit.getVersion())
                }

                return field
            }

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
            return currentVersion!!.isAtLeast(version)
        }
    }
}