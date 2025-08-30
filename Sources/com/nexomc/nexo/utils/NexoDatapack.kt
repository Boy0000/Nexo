package com.nexomc.nexo.utils

import com.nexomc.nexo.NexoBootstrap
import com.nexomc.nexo.nms.NMSHandlers
import com.nexomc.nexo.recipes.RecipesManager
import com.nexomc.nexo.utils.JsonBuilder.plus
import com.nexomc.nexo.utils.logs.Logs
import org.bukkit.Bukkit
import org.bukkit.World

open class NexoDatapack(key: String, description: String) {

    companion object {
        private val removedDatapacks = listOfNotNull(
            if (NexoBootstrap.bootsStrung) "nexo_custom_blocks" else null,
            if (NexoBootstrap.bootsStrung) "nexo_music_discs" else null,
        )

        fun clearOldDatapacks() {
            Bukkit.getServer().worldContainer.listFiles { it.isDirectory }.toList().resolve("datapacks")
                .flatMap { it.listFiles { it.isDirectory && it.name in removedDatapacks }.toList() }
                .forEach { it.deleteRecursively() }
        }
    }

    val defaultWorld: World = Bukkit.getWorlds().first()
    val datapackName = "file/$key"
    val datapackFile = defaultWorld.worldFolder.resolve("datapacks/$key")
    val isFirstInstall: Boolean get() = Bukkit.getDatapackManager().packs.map { it.name }.none(datapackName::equals)
    val datapackEnabled: Boolean get() = Bukkit.getDatapackManager().packs.find { it.name == datapackName }?.isEnabled ?: false
    private val datapackMeta = JsonBuilder.jsonObject.plus("pack", JsonBuilder.jsonObject.plus("description", description).plus("pack_format", NMSHandlers.handler().datapackFormat()))

    init {
        datapackFile.resolve("data").deleteRecursively()
    }

    fun writeMCMeta() {
        datapackFile.resolve("pack.mcmeta").apply {
            parentFile.mkdirs()
        }.writeText(datapackMeta.toString())
    }

    internal fun enableDatapack(enabled: Boolean, reload: Boolean = false) {
        SchedulerUtils.launchDelayed {
            if (VersionUtil.below("1.21.1")) return@launchDelayed Logs.logWarn("Could not enable $datapackName datapack, use /datapack-command")
            Bukkit.getDatapackManager().getPack(datapackName)?.takeUnless { it.isEnabled == enabled }?.also {
                it.isEnabled = enabled
                if (reload) RecipesManager.reload()
            }
        }
    }
}
