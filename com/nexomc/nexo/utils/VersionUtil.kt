package com.nexomc.nexo.utils

import com.nexomc.nexo.utils.JarReader.manifestMap
import com.nexomc.nexo.utils.logs.Logs
import org.apache.commons.lang3.Validate
import org.bukkit.Bukkit

object VersionUtil {
    private val versionMap = mutableMapOf<NMSVersion, Map<Int, MinecraftVersion>>()

    init {
        versionMap[NMSVersion.v1_21_R3] = mapOf(8 to MinecraftVersion("1.21.4"))
        versionMap[NMSVersion.v1_21_R2] = mapOf(6 to MinecraftVersion("1.21.2"), 7 to MinecraftVersion("1.21.3"))
        versionMap[NMSVersion.v1_21_R1] = mapOf(4 to MinecraftVersion("1.21"), 5 to MinecraftVersion("1.21.1"))
        versionMap[NMSVersion.v1_20_R4] = mapOf(2 to MinecraftVersion("1.20.5"), 3 to MinecraftVersion("1.20.6"))
        versionMap[NMSVersion.v1_20_R3] = mapOf(1 to MinecraftVersion("1.20.4"))
    }

    fun MinecraftVersion.toNMS() =
        versionMap.entries.firstOrNull { it.value.containsValue(this) }?.key ?: NMSVersion.UNKNOWN

    fun matchesServer(server: String) = MinecraftVersion.currentVersion == MinecraftVersion(server)

    @JvmStatic
    fun atleast(versionString: String): Boolean {
        return MinecraftVersion(versionString).atOrAbove()
    }

    fun below(versionString: String) = !atleast(versionString)

    /**
     * @return true if the server is Paper or false of not
     * @throws IllegalArgumentException if server is null
     */
    val isPaperServer: Boolean
        get() {
            val server = Bukkit.getServer()
            Validate.notNull(server, "Server cannot be null")
            if (server.name.equals("Paper", ignoreCase = true)) return true

            return runCatching {
                Class.forName("com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent")
            }.getOrNull() != null
        }

    val isFoliaServer: Boolean
        get() {
            val server = Bukkit.getServer()
            Validate.notNull(server, "Server cannot be null")

            return server.name.equals("Folia", ignoreCase = true)
        }

    fun isSupportedVersion(serverVersion: NMSVersion, vararg supportedVersions: NMSVersion): Boolean {
        for (version in supportedVersions) if (version == serverVersion) return true

        Logs.logWarn("The Server version which you are running is unsupported, you are running version '$serverVersion'.")
        Logs.logWarn("The plugin supports following versions ${combineVersions(*supportedVersions)}.")

        if (serverVersion == NMSVersion.UNKNOWN) {
            Logs.logWarn("The Version '$serverVersion' can indicate, that you are using a newer Minecraft version than currently supported.")
            Logs.logWarn("In this case please update to the newest version of this plugin. If this is the newest Version, than please be patient. It can take a few weeks until the plugin is updated.")
        }

        Logs.logWarn("No compatible Server version found!")

        return false
    }

    private fun combineVersions(vararg versions: NMSVersion): String {
        return buildString {
            versions.forEachIndexed { i, version ->
                if (i == 0) append(" ")
                append("'")
                append(version)
                append("'")
            }
        }
    }

    val isCompiled: Boolean
        get() = manifestMap.isEmpty() || ((manifestMap["Compiled"]?.toBoolean() == true) && !isValidCompiler)

    val isCI: Boolean
        get() = manifestMap["CI"]?.toBoolean() == true

    private val isValidCompiler: Boolean
        get() = manifestMap["Built-By"]?.equals("sivert", ignoreCase = true) == true

    val isLeaked = JarReader.checkIsLeaked()

    enum class NMSVersion {
        v1_21_R3, v1_21_R2, v1_21_R1, v1_20_R4, v1_20_R3, UNKNOWN;

        companion object {
            fun matchesServer(version: NMSVersion) =
                version != UNKNOWN && MinecraftVersion.currentVersion?.toNMS() == version
        }
    }
}
