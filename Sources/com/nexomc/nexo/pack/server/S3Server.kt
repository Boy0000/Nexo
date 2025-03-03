package com.nexomc.nexo.pack.server

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.logs.Logs
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import org.bukkit.entity.Player
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.net.URI
import java.util.*
import java.util.concurrent.CompletableFuture

class S3Server : NexoPackServer {

    private val bucket = Settings.S3_BUCKET_NAME.toString()
    private var packUrl: String? = Settings.S3_PUBLIC_URL.toString()
    private var packURI: URI? = URI.create(packUrl)
    private var hash: String? = null
    private var packUUID: UUID? = null
    private var uploadFuture: CompletableFuture<Void>? = null

    val s3Client: S3Client = S3Client.builder()
        .region(runCatching {
            Region.of(Settings.S3_REGION.toString())
        }.onFailure {
            Logs.logWarn("Failed to find Region for ${Settings.S3_REGION}, defaulting to ${Region.EU_WEST_1.id()}")
            if (Settings.DEBUG.toBool()) it.printStackTrace()
        }.getOrDefault(Region.EU_WEST_1))
        .credentialsProvider { AwsBasicCredentials.create(Settings.S3_ACCESS_KEY.toString(), Settings.S3_SECRET_KEY.toString()) }
        .endpointOverride(packURI)
        .build()

    override fun packUrl() = packUrl ?: ""

    override val isPackUploaded: Boolean
        get() = NexoPlugin.instance().packGenerator().packGenFuture?.isDone != false && uploadFuture?.isDone != false

    override fun uploadPack(): CompletableFuture<Void> {
        val builtPack = NexoPlugin.instance().packGenerator().builtPack()!!
        if (hash != builtPack.hash()) {
            uploadFuture?.cancel(true)
            uploadFuture = null
        }

        if (uploadFuture == null) uploadFuture = CompletableFuture.runAsync {
            runCatching {
                val putRequest = PutObjectRequest.builder().bucket(bucket).key(Settings.S3_KEY.toString()).build()
                s3Client.putObject(putRequest, RequestBody.fromBytes(builtPack.data().toByteArray()))
                val putRequestHash = PutObjectRequest.builder().bucket(bucket).key(builtPack.hash()).build()
                s3Client.putObject(putRequestHash, RequestBody.fromString(builtPack.hash()))
            }.onFailure {
                Logs.logError("The resource pack has not been uploaded to the S3-Server")
                if (Settings.DEBUG.toBool()) it.printStackTrace()
                else Logs.logWarn(it.message!!)
            }
        }

        return uploadFuture!!
    }

    override fun sendPack(player: Player) {
        if (NexoPlugin.instance().packGenerator().packGenFuture?.isDone == false) return
        if (uploadFuture == null || uploadFuture?.isDone == false) return

        val request = ResourcePackRequest.resourcePackRequest()
            .required(NexoPackServer.mandatory).replace(true)
            .prompt(NexoPackServer.prompt).packs(ResourcePackInfo.resourcePackInfo(packUUID!!, packURI!!, hash!!))
            .build()
        player.sendResourcePacks(request)
    }
}