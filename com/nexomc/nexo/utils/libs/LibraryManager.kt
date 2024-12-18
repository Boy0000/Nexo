package com.nexomc.nexo.utils.libs

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.VersionUtil.NMSVersion.Companion.matchesServer
import net.byteflux.libby.BukkitLibraryManager
import net.byteflux.libby.Library

class LibraryManager {
    var libs: ArrayList<Library> = ArrayList()

    var COMMAND_API_VERSION = "9.7.0"
    var CREATIVE_VERSION = "1.7.3"
    var IDOFRONT_VERSION = "0.25.17"

    fun load() {
        if (matchesServer(VersionUtil.NMSVersion.v1_20_R3) || !VersionUtil.isPaperServer) {
            libs.add(getLib("dev{}jorel", "commandapi-bukkit-shade", COMMAND_API_VERSION).build())
        } else {
            libs.add(getLib("dev{}jorel", "commandapi-bukkit-shade-mojang-mapped", COMMAND_API_VERSION).build())
        }
        libs.add(getLib("dev{}jorel", "commandapi-bukkit-kotlin", COMMAND_API_VERSION).build())

        libs.add(getLib("com{}jeff-media", "custom-block-data", "2.2.2").build())
        libs.add(getLib("com{}jeff-media", "MorePersistentDataTypes", "2.4.0").build())
        libs.add(getLib("com{}jeff-media", "persistent-data-serializer", "1.0").build())

        libs.add(getLib("team{}unnamed", "creative-api", "1.7.6-SNAPSHOT")
            .url("https://repo.nexomc.com/snapshots/team/unnamed/creative-api/1.7.6-SNAPSHOT/creative-api-1.7.6-SNAPSHOT.jar")
            .build())
        libs.add(getLib("team{}unnamed", "creative-server", CREATIVE_VERSION).build())
        libs.add(getLib("team{}unnamed", "creative-serializer-minecraft", "1.7.6-SNAPSHOT")
            .url("https://repo.nexomc.com/snapshots/team/unnamed/creative-serializer-minecraft/1.7.6-SNAPSHOT/creative-serializer-minecraft-1.7.6-SNAPSHOT.jar")
            .build())

        libs.add(getLib("io{}th0rgal", "protectionlib", "1.8.0").build())
        libs.add(getLib("com{}tcoded", "FoliaLib", "0.4.3").build())

        libs.add(getLib("me{}gabytm{}util", "actions-spigot", "1.0.0-SNAPSHOT").build())
        libs.add(getLib("me{}gabytm{}util", "actions-core", "1.0.0-SNAPSHOT").build())

        libs.add(getLib("dev{}triumphteam", "triumph-gui", "3.1.10").build())
        libs.add(getLib("com{}github{}stefvanschie{}inventoryframework", "IF", "0.10.19").build())

        libs.add(getLib("com{}mineinabyss", "idofront-util", IDOFRONT_VERSION).build())
    }

    fun getLib(groupID: String, artifact: String, version: String): Library.Builder {
        return Library.builder()
            .groupId(groupID)
            .artifactId(artifact)
            .version(version)
            .relocate(groupID, "com.nexomc.libs.$groupID")
            .isolatedLoad(false)
    }

    fun loadLibs(plugin: NexoPlugin) {
        println("Loading libraries...")

        val manager = BukkitLibraryManager(plugin)

        manager.addMavenCentral()
        manager.addJitPack()
        manager.addRepository("https://repo.nexomc.com/releases/")
        manager.addRepository("https://repo.nexomc.com/snapshots/")
        manager.addRepository("https://repo.mineinabyss.com/releases/")
        manager.addRepository("https://repo.mineinabyss.com/snapshots/")
        manager.addRepository("https://repo.triumphteam.dev/snapshots")
        manager.addRepository("https://repo.oraxen.com/releases/")

        load()
        libs.forEach {
            println("Loading library: ${it.artifactId}")
            manager.loadLibrary(it)
        }
    }
}