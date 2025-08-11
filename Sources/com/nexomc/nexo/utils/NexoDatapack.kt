package com.nexomc.nexo.utils

import com.nexomc.nexo.NexoBootstrap
import com.nexomc.nexo.nms.NMSHandlers
import com.nexomc.nexo.recipes.RecipesManager
import com.nexomc.nexo.utils.JsonBuilder.plus
import com.nexomc.nexo.utils.logs.Logs
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.lifecycle.event.LifecycleEvent
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventOwner
import io.papermc.paper.plugin.lifecycle.event.handler.LifecycleEventHandler
import io.papermc.paper.plugin.lifecycle.event.handler.configuration.LifecycleEventHandlerConfiguration
import io.papermc.paper.plugin.lifecycle.event.handler.configuration.PrioritizedLifecycleEventHandlerConfiguration
import io.papermc.paper.registry.RegistryBuilder
import io.papermc.paper.registry.event.RegistryComposeEvent
import io.papermc.paper.registry.event.RegistryEventProvider
import org.bukkit.Bukkit
import org.bukkit.World

fun <A : Any, B : RegistryBuilder<A>> RegistryEventProvider<A, B>.handler(
    h: LifecycleEventHandler<in RegistryComposeEvent<A, B>>
): PrioritizedLifecycleEventHandlerConfiguration<BootstrapContext> {
    val eventType = if (VersionUtil.atleast("1.21.6")) compose() else freeze()
    return eventType.newHandler(h)
}

open class NexoDatapack(key: String, description: String) {

    companion object {
        private val removedDatapacks = listOfNotNull(
            if (NexoBootstrap.bootsStrung) "nexo_custom_blocks" else null,
            if (NexoBootstrap.bootsStrung) "nexo_music_discs" else null,
        )

        fun clearOldDatapacks() {
            Bukkit.getServer().worldContainer.walkBottomUp().filter {
                it.isDirectory && it.name in removedDatapacks
            }.forEach { it.deleteRecursively() }
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
        SchedulerUtils.callSyncMethod {
            if (VersionUtil.below("1.21.1")) return@callSyncMethod Logs.logWarn("Could not enable $datapackName datapack, use /datapack-command")
            Bukkit.getDatapackManager().getPack(datapackName)?.takeUnless { it.isEnabled == enabled }?.also {
                it.isEnabled = enabled
                if (reload) RecipesManager.reload()
            }
        }
    }
}
