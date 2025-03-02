package com.nexomc.nexo.pack.server

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.pack.PackListener
import com.nexomc.nexo.utils.AdventureUtils
import com.nexomc.nexo.utils.logs.Logs
import net.kyori.adventure.resource.ResourcePackInfo
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import java.net.URI
import java.util.*
import java.util.concurrent.CompletableFuture

interface NexoPackServer {
    val isPackUploaded: Boolean
        get() = uploadPack().isDone

    fun uploadPack(): CompletableFuture<Void> {
        return CompletableFuture.allOf(NexoPlugin.instance().packGenerator().packGenFuture)
    }

    fun sendPack(player: Player)

    fun start() {
    }

    fun stop() {
    }

    fun packUrl(): String

    fun packInfo(): ResourcePackInfo? {
        val hash = NexoPlugin.instance().packGenerator().builtPack()?.hash() ?: return null
        return ResourcePackInfo.resourcePackInfo()
            .hash(hash)
            .id(UUID.nameUUIDFromBytes(hashArray(hash)))
            .uri(URI.create(packUrl()))
            .build()
    }

    companion object {
        @JvmStatic
        fun initializeServer(): NexoPackServer {
            NexoPlugin.instance().packServer().stop()
            HandlerList.unregisterAll(packListener)
            Bukkit.getPluginManager().registerEvents(packListener, NexoPlugin.instance())

            val type = Settings.PACK_SERVER_TYPE.toEnumOrGet(PackServerType::class.java) { serverType: String ->
                Logs.logError("Invalid PackServer-type specified: $serverType")
                Logs.logError("Valid types are: ${PackServerType.entries.joinToString()}, defaulting to ${PackServerType.NONE}")
                PackServerType.NONE
            }

            Logs.logInfo("PackServer set to " + type.name)

            return when (type) {
                PackServerType.SELFHOST -> SelfHostServer()
                PackServerType.POLYMATH -> PolymathServer()
                PackServerType.LOBFILE -> LobFileServer()
                PackServerType.NONE -> EmptyServer()
            }
        }

        fun hashArray(hash: String): ByteArray {
            require(hash.length % 2 == 0) { "Hash length must be even" }
            return ByteArray(hash.length / 2) { i ->
                hash.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        }


        private val packListener = PackListener()
        val mandatory = Settings.PACK_SEND_MANDATORY.toBool()
        val prompt = AdventureUtils.MINI_MESSAGE.deserialize(Settings.PACK_SEND_PROMPT.toString())
    }
}
