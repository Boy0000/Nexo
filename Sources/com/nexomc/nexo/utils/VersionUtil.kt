package com.nexomc.nexo.utils

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.utils.JarReader.manifestMap
import com.nexomc.nexo.utils.VersionUtil.NMSVersion.UNKNOWN
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap

object VersionUtil {
    private val versionMap = Object2ObjectOpenHashMap<NMSVersion, Map<Int, MinecraftVersion>>()

    init {
        versionMap[NMSVersion.v1_21_R4] = mapOf(9 to MinecraftVersion("1.21.5"))
        versionMap[NMSVersion.v1_21_R3] = mapOf(8 to MinecraftVersion("1.21.4"))
        versionMap[NMSVersion.v1_21_R2] = mapOf(6 to MinecraftVersion("1.21.2"), 7 to MinecraftVersion("1.21.3"))
        versionMap[NMSVersion.v1_21_R1] = mapOf(4 to MinecraftVersion("1.21"), 5 to MinecraftVersion("1.21.1"))
        versionMap[NMSVersion.v1_20_R4] = mapOf(2 to MinecraftVersion("1.20.5"), 3 to MinecraftVersion("1.20.6"))
        versionMap[NMSVersion.v1_20_R3] = mapOf(1 to MinecraftVersion("1.20.4"))
    }

    fun MinecraftVersion.toNMS() = versionMap.entries.find { it.value.containsValue(this) }?.key ?: UNKNOWN

    fun matchesServer(server: String) = MinecraftVersion.currentVersion == MinecraftVersion(server)

    @JvmStatic
    fun atleast(versionString: String): Boolean {
        return MinecraftVersion(versionString).atOrAbove()
    }

    fun below(versionString: String) = !atleast(versionString)

    val isFoliaServer: Boolean = NexoPlugin.instance().foliaLib.isFolia

    val isCompiled by lazy { manifestMap.isEmpty() || ((manifestMap["Compiled"]?.toBoolean() == true) && !isValidCompiler) }

    val isDevBuild: Boolean by lazy { manifestMap.isEmpty() || ((manifestMap["Devbuild"]?.toBoolean() == true) && isValidCompiler) }

    val isCI: Boolean by lazy { manifestMap["CI"]?.toBoolean() == true }

    private val isValidCompiler: Boolean by lazy { manifestMap["Built-By"]?.equals("sivert", ignoreCase = true) == true }

    val isLeaked = JarReader.checkIsLeaked()

    enum class NMSVersion {
        v1_21_R4, v1_21_R3, v1_21_R2, v1_21_R1, v1_20_R4, v1_20_R3, UNKNOWN;
    }

    fun matchesServer(version: NMSVersion) =
        version != UNKNOWN && MinecraftVersion.currentVersion.toNMS() == version
}
