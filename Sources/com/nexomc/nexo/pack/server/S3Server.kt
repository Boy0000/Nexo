package com.nexomc.nexo.pack.server

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.SchedulerUtils
import com.nexomc.nexo.utils.logs.Logs
import kotlinx.coroutines.Job
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.net.URI
import kotlin.time.Duration
import kotlin.time.toJavaDuration

open class S3Server : NexoPackServer {
    internal open val bucket = Settings.S3_BUCKET_NAME.toString()
    internal open val accessKey = Settings.S3_ACCESS_KEY.toString()
    internal open val secretKey = Settings.S3_SECRET_KEY.toString()
    internal open val region = runCatching {
        Region.of(Settings.S3_REGION.toString("auto"))
    }.onFailure {
        Logs.logWarn("Failed to find Region for ${Settings.S3_REGION}, defaulting to ${Region.EU_WEST_1.id()}")
        if (Settings.DEBUG.toBool()) it.printStackTrace()
    }.getOrDefault(Region.EU_WEST_1)
    internal open val urlExpiration: Duration = Settings.S3_URL_EXPIRATION.toDuration()

    private var packUrl: String? = null
    internal open val endpoint: URI = URI.create(Settings.S3_PUBLIC_URL.toString())
    private var hash: String? = null
    private var uploadFuture: Job? = null

    val s3Client: S3Client by lazy {
        S3Client.builder()
            .region(region)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
            .endpointOverride(endpoint)
            .forcePathStyle(true) // Add for Hetzner compatibility
            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
            .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
            .build()
    }

    override fun stop() {
        super.stop()
        s3Client.close()
    }

    override fun packUrl(): String {
        if (uploadFuture?.isCompleted != true) return ""

        return runCatching {
            S3Presigner.builder()
                .region(region)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .endpointOverride(endpoint)
                .build().use { presigner ->
                    val key = Settings.S3_UNIQUE_KEY.toString("resourcepacks/$hash.zip")
                    val presignRequest = GetObjectPresignRequest.builder()
                        .signatureDuration(urlExpiration.coerceAtLeast(Duration.ZERO).toJavaDuration())
                        .getObjectRequest { builder ->
                            builder.bucket(bucket).key(key)
                        }.build()

                    presigner.presignGetObject(presignRequest).url().toString().apply { packUrl = this }
                }
        }.onFailure {
            Logs.logError("Failed to generate pre-signed ResourcePack URL: ${it.message}")
            if (Settings.DEBUG.toBool()) it.printStackTrace()
        }.getOrDefault("")
    }

    override val isPackUploaded: Boolean
        get() = NexoPlugin.instance().packGenerator().packGenJob?.isCompleted != false && uploadFuture?.isCompleted != false

    override fun uploadPack(): Job {
        val builtPack = NexoPlugin.instance().packGenerator().builtPack()!!
        if (hash != builtPack.hash()) {
            uploadFuture?.cancel()
            uploadFuture = null
        }

        if (uploadFuture == null) uploadFuture = SchedulerUtils.launch {
            runCatching {
                this@S3Server.hash = builtPack.hash()
                val key = Settings.S3_UNIQUE_KEY.toString("resourcepacks/$hash.zip")

                // If there is no hard-specified unique-key, check if the object exists under the given key
                val exists = Settings.S3_UNIQUE_KEY.value == null && runCatching {
                    s3Client.headObject { it.bucket(bucket).key(key) }
                }.isSuccess

                if (exists) {
                    Logs.logInfo("ResourcePack with hash '${builtPack.hash()}' already exists on the S3-Server. Skipping upload...")
                } else {
                    val putRequest = PutObjectRequest.builder().bucket(bucket).key(key).build()
                    s3Client.putObject(putRequest, RequestBody.fromBytes(builtPack.data().toByteArray()))

                    val putRequestHash = PutObjectRequest.builder().bucket(bucket).key("hashes/" + builtPack.hash()).build()
                    s3Client.putObject(putRequestHash, RequestBody.fromString(builtPack.hash()))
                    Logs.logInfo("Uploaded resource pack to S3-Server with key: $key")
                }
            }.onFailure {
                Logs.logError("Failed to upload resource pack to S3: " + it.message)
                if (Settings.DEBUG.toBool()) it.printStackTrace()
            }
        }

        return uploadFuture!!
    }
}