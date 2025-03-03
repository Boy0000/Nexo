package com.nexomc.nexo.pack.server

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.logs.Logs
import com.sun.net.httpserver.HttpExchange
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import org.bukkit.entity.Player
import team.unnamed.creative.server.ResourcePackServer
import team.unnamed.creative.server.handler.ResourcePackRequestHandler

class SelfHostServer : NexoPackServer {
    private var packServer: ResourcePackServer? = null
    private val publicAddress: String

    private var builtPackArray: ByteArray? = null
    private val handler = ResourcePackRequestHandler { _, exchange: HttpExchange ->
        val packData = NexoPlugin.instance().packGenerator().builtPack()!!.data()
        if (builtPackArray == null) builtPackArray = packData.toByteArray()
        exchange.responseHeaders["Content-Type"] = listOf("application/zip")
        exchange.sendResponseHeaders(200, builtPackArray!!.size.toLong())
        exchange.responseBody.write(builtPackArray!!)
    }

    init {
        this.publicAddress = publicAddress()
        runCatching {
            packServer = ResourcePackServer.server().address(Settings.SELFHOST_PACK_SERVER_PORT.toInt(8082))
                .executor(Executors.newFixedThreadPool(Settings.SELFHOST_DISPATCH_THREADS.toInt(10))).handler(handler).build()
        }.onFailure {
            if (Settings.DEBUG.toBool()) it.printStackTrace()
            else Logs.logWarn(it.message!!, true)
            Logs.logError("Failed to start Nexo pack-server")
        }
    }

    override fun packUrl(): String {
        val hash = NexoPlugin.instance().packGenerator().builtPack()!!.hash()
        val serverPort = Settings.SELFHOST_PACK_SERVER_PORT.toInt(8082)
        val address = publicAddress.takeIf { publicAddress.startsWith("http") } ?: "http://$publicAddress:$serverPort"
        return "$address/$hash.zip"
    }

    override fun sendPack(player: Player) {
        NexoPlugin.instance().packGenerator().packGenFuture?.thenRun {
            val hash = NexoPlugin.instance().packGenerator().builtPack()!!.hash()
            val packUUID = UUID.nameUUIDFromBytes(NexoPackServer.hashArray(hash))
            val packUrl = URI.create(packUrl())

            val request = ResourcePackRequest.resourcePackRequest()
                .required(NexoPackServer.mandatory).replace(true).prompt(NexoPackServer.prompt)
                .packs(ResourcePackInfo.resourcePackInfo(packUUID, packUrl, hash)).build()
            player.sendResourcePacks(request)
        }
    }

    override val isPackUploaded: Boolean
        get() = true

    override fun uploadPack(): CompletableFuture<Void> {
        val hashPart = "/${NexoPlugin.instance().packGenerator().builtPack()!!.hash()}.zip"
        val address = publicAddress.takeIf { it.startsWith("http") } ?: "http://$publicAddress:${packServer!!.address().port}"
        if (Settings.DEBUG.toBool()) Logs.logSuccess("Resourcepack uploaded and will be dispatched with publicAddress $address$hashPart")
        else Logs.logSuccess("Resourcepack has been uploaded to SelfHost!")

        return CompletableFuture.completedFuture(null)
    }

    override fun start() {
        if (packServer == null) return
        Logs.logSuccess("Started Self-Host Pack-Server...")
        packServer!!.start()
    }

    override fun stop() {
        if (packServer == null) return
        Logs.logError("Stopping Self-Host Pack-Server...")
        packServer!!.stop(0)
        packServer = null
    }

    private fun publicAddress(): String {
        val urlString = "http://checkip.amazonaws.com/"
        val publicAddress = runCatching {
            val url = URI.create(urlString).toURL()
            BufferedReader(InputStreamReader(url.openStream())).use { br ->
                br.readLine()
            }
        }.onFailure {
            if (Settings.DEBUG.toBool()) it.printStackTrace()
            Logs.logError("Failed to get publicAddress for SELFHOST server...")
            Logs.logWarn("You can manually set it in `settings.yml` at ")
        }.getOrNull() ?: "0.0.0.0"
        return Settings.SELFHOST_PUBLIC_ADDRESS.toString(publicAddress)
    }
}
