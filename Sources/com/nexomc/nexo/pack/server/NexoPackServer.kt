package com.nexomc.nexo.pack.server

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.pack.PackListener
import com.nexomc.nexo.utils.SchedulerUtils
import com.nexomc.nexo.utils.logs.Logs
import io.papermc.paper.connection.PlayerConfigurationConnection
import kotlinx.coroutines.Job
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import java.net.URI
import java.util.*

interface NexoPackServer {
    val isPackUploaded: Boolean get() = uploadPack().isCompleted

    fun uploadPack(): Job {
        return NexoPlugin.instance().packGenerator().packGenJob ?: Job()
    }

    fun sendPack(connection: Any, reconfigure: Boolean): Job? {
        val connection = connection as? PlayerConfigurationConnection ?: return null
        val job = if (reconfigure) null else Job()
        val info = NexoPlugin.instance().packServer().packInfo() ?: return null
        val request = ResourcePackRequest.resourcePackRequest()
            .required(mandatory).replace(true).prompt(prompt)
            .packs(info).callback { uuid, status, audience ->
                if (!status.intermediate()) job?.complete() ?: connection.completeReconfiguration()
            }.build()

        connection.audience.sendResourcePacks(request)
        return job
    }

    fun sendPack(player: Player) {
        SchedulerUtils.launch {
            NexoPlugin.instance().packGenerator().packGenJob?.join()
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

        var mandatory = runCatching { Settings.PACK_SEND_MANDATORY.toBool() }.getOrDefault(true); private set
        var prompt: Component? = null; private set

        fun registerDefaultPackServers() {
            PackServerRegistry.register("SELFHOST", ::SelfHostServer)
            PackServerRegistry.register("POLYMATH", ::PolymathServer)
            PackServerRegistry.register("LOBFILE", ::LobFileServer)
            PackServerRegistry.register("S3", ::S3Server)
            PackServerRegistry.register("NEXO", ::NexoServer)
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
