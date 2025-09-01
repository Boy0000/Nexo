package com.nexomc.nexo.utils

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.utils.JarReader.manifestMap
import com.nexomc.nexo.utils.VersionUtil.NMSVersion.UNKNOWN
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap

@Suppress("EnumEntryName")
object VersionUtil {
    private val versionMap = Object2ObjectOpenHashMap<NMSVersion, Array<MinecraftVersion>>()

    init {
        versionMap[NMSVersion.v1_21_R6] = arrayOf(MinecraftVersion("1.21.6"), MinecraftVersion("1.21.7"), MinecraftVersion("1.21.8"))
        versionMap[NMSVersion.v1_21_R4] = arrayOf(MinecraftVersion("1.21.5"))
        versionMap[NMSVersion.v1_21_R3] = arrayOf(MinecraftVersion("1.21.4"))
        versionMap[NMSVersion.v1_21_R2] = arrayOf(MinecraftVersion("1.21.2"), MinecraftVersion("1.21.3"))
        versionMap[NMSVersion.v1_21_R1] = arrayOf(MinecraftVersion("1.21"), MinecraftVersion("1.21.1"))
        versionMap[NMSVersion.v1_20_R4] = arrayOf(MinecraftVersion("1.20.5"), MinecraftVersion("1.20.6"))
        versionMap[NMSVersion.v1_20_R3] = arrayOf(MinecraftVersion("1.20.4"))
    }

    fun MinecraftVersion.toNMS() = versionMap.entries.find { this in it.value }?.key ?: UNKNOWN

    fun matchesServer(server: String) = MinecraftVersion.currentVersion == MinecraftVersion(server)

    @JvmStatic
    fun atleast(versionString: String): Boolean {
        return MinecraftVersion(versionString).atOrAbove()
    }

    fun below(versionString: String) = !atleast(versionString)

    val isFoliaServer by lazy { NexoPlugin.instance().foliaLib.isFolia }

    val isCompiled by lazy { manifestMap.isEmpty() || ((manifestMap["Compiled"]?.toBoolean() == true) && !isValidCompiler) }

    val isDevBuild: Boolean by lazy { manifestMap.isEmpty() || ((manifestMap["Devbuild"]?.toBoolean() == true) && isValidCompiler) }

    val isCI: Boolean by lazy { manifestMap["CI"]?.toBoolean() == true }

    private val isValidCompiler by lazy { manifestMap["Built-By"]?.equals("sivert", ignoreCase = true) == true }

    private var _leaked: Boolean? = null
    var isLeaked: Boolean
        get() {
            if (_leaked ==  null) _leaked = JarReader.checkIsLeaked()
            return _leaked!!
        }
        internal set(value) {
            _leaked = value
            NoticeUtils.leakNotice()
        }

    enum class NMSVersion {
        v1_21_R6, v1_21_R4, v1_21_R3, v1_21_R2, v1_21_R1, v1_20_R4, v1_20_R3, UNKNOWN;
    }

    fun matchesServer(version: NMSVersion) =
        version != UNKNOWN && MinecraftVersion.currentVersion.toNMS() == version
}
