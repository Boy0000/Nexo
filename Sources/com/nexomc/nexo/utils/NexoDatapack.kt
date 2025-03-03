package com.nexomc.nexo.utils

import com.nexomc.nexo.nms.NMSHandlers
import com.nexomc.nexo.utils.JsonBuilder.plus
import com.nexomc.nexo.utils.logs.Logs
import org.bukkit.Bukkit
import org.bukkit.World

open class NexoDatapack(key: String, description: String) {

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

    internal fun enableDatapack(enabled: Boolean) {
        SchedulerUtils.callSyncMethod {
            if (VersionUtil.below("1.21.1")) return@callSyncMethod Logs.logWarn("Could not enable $datapackName datapack, use /datapack-command")
            //Bukkit.getDatapackManager().refreshPacks()
            Bukkit.getDatapackManager().getPack(datapackName)?.takeUnless { it.isEnabled == enabled }?.isEnabled = enabled
        }
    }
}
