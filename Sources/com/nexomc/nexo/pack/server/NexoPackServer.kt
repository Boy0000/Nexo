package com.nexomc.nexo.pack.server

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.pack.PackListener
import com.nexomc.nexo.utils.logs.Logs
import io.papermc.paper.connection.PlayerConfigurationConnection
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import java.net.URI
import java.util.*
import java.util.concurrent.CompletableFuture

interface NexoPackServer {
    val isPackUploaded: Boolean get() = uploadPack().isDone

    fun uploadPack(): CompletableFuture<Void> {
        return CompletableFuture.allOf(NexoPlugin.instance().packGenerator().packGenFuture)
    }

    fun sendPack(connection: Any, reconfigure: Boolean): CompletableFuture<Void>? {
        val connection = connection as? PlayerConfigurationConnection ?: return null
        val future = if (reconfigure) null else CompletableFuture<Void>()
        val info = NexoPlugin.instance().packServer().packInfo() ?: return null
        val request = ResourcePackRequest.resourcePackRequest()
            .required(mandatory).replace(true).prompt(prompt)
            .packs(info).callback { uuid, status, audience ->
                if (!status.intermediate()) future?.complete(null) ?: connection.completeReconfiguration()
            }.build()

        connection.audience.sendResourcePacks(request)
        return future
    }

    fun sendPack(player: Player) {
        NexoPlugin.instance().packGenerator().packGenFuture?.thenRun {
            val hash = NexoPlugin.instance().packGenerator().builtPack()!!.hash()
            val packUUID = UUID.nameUUIDFromBytes(hashArray(hash))
            val packUrl = URI.create(packUrl())

            val request = ResourcePackRequest.resourcePackRequest()
                .required(mandatory && !player.hasPermission(BYPASS_PERMISSION)).replace(true).prompt(prompt)
                .packs(ResourcePackInfo.resourcePackInfo(packUUID, packUrl, hash)).build()
            player.sendResourcePacks(request)
        }
    }

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
        const val BYPASS_PERMISSION = "nexo.resourcepack.bypass"

        var mandatory = Settings.PACK_SEND_MANDATORY.toBool(); private set
        var prompt = Settings.PACK_SEND_PROMPT.toComponent(); private set

        fun registerDefaultPackServers() {
            PackServerRegistry.register("SELFHOST", ::SelfHostServer)
            PackServerRegistry.register("POLYMATH", ::PolymathServer)
            PackServerRegistry.register("LOBFILE", ::LobFileServer)
            PackServerRegistry.register("S3", ::S3Server)
            PackServerRegistry.register("NONE", ::EmptyServer)
        }

        @JvmStatic
        fun initializeServer() {
            NexoPlugin.instance().packServer().stop()
            HandlerList.unregisterAll(packListener)
            Bukkit.getPluginManager().registerEvents(packListener, NexoPlugin.instance())

            val type = Settings.PACK_SERVER_TYPE.toString().uppercase()
            val server = PackServerRegistry.create(type) ?: run {
                Logs.logError("Invalid PackServer type specified: $type")
                Logs.logError("Valid types are: ${PackServerRegistry.allRegisteredTypes.joinToString()}")
                EmptyServer()
            }

            mandatory = Settings.PACK_SEND_MANDATORY.toBool()
            prompt = Settings.PACK_SEND_PROMPT.toComponent()

            Logs.logInfo("PackServer set to $type")
            NexoPlugin.instance().packServer(server)
        }

        fun hashArray(hash: String): ByteArray {
            require(hash.length % 2 == 0) { "Hash length must be even" }
            return ByteArray(hash.length / 2) { i ->
                hash.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        }


        private val packListener = PackListener()
    }
}
