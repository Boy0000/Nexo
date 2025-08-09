package com.nexomc.nexo.pack.server

import com.google.gson.JsonParser
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.logs.Logs
import software.amazon.awssdk.regions.Region
import java.io.File
import java.net.URI

class NexoServer : S3Server() {
    private val fallback = PolymathServer()
    private val credentials: Credentials = _credentials ?: Credentials("", Region.AWS_GLOBAL, "", "", "")

    override val accessKey: String = credentials.accessKey
    override val secretKey: String = credentials.secretKey
    override val endpoint: URI = URI.create(credentials.url)
    override val bucket = credentials.bucket
    override val region = credentials.region

    override fun start() {
        //Test connection to Hetzner S3 bucket
        runCatching { s3Client.headBucket { it.bucket(bucket).build() } }.onFailure {
            Logs.logError("Failed to connect to Nexo's ResourcePack server...")
            Logs.logWarn("Will fall back to old Polymath server in the meantime")
            if (Settings.DEBUG.toBool()) it.printStackTrace()

            NexoPlugin.instance().packServer(fallback)
        }
    }

    companion object {
        private var _credentials: Credentials? = null
            get() {
                if (field == null) field = readCredentials()
                return field
            }
        private fun readCredentials(): Credentials? {
            if (VersionUtil.isLeaked || VersionUtil.isCompiled || VersionUtil.isCI) return null

            val credJson = NexoPlugin.instance().dataFolder.resolve("cred.secret").takeIf(File::exists)?.readText()
                ?: NexoPlugin.instance().getResource("cred.secret")?.readAllBytes()?.decodeToString()

            if (credJson == null) {
                Logs.logError("Could not find credentials for NexoPackServer...")
                return null
            }

            return runCatching {
                val credJson = JsonParser.parseString(credJson).asJsonObject
                Credentials(
                    credJson.get("url").asString,
                    runCatching { Region.of(credJson.get("region").asString) }.getOrDefault(Region.EU_CENTRAL_1),
                    credJson.get("bucket").asString,
                    credJson.get("access").asString,
                    credJson.get("secret").asString
                )
            }.getOrNull()
        }
    }

    private data class Credentials(val url: String, val region: Region, val bucket: String, val accessKey: String, val secretKey: String)
}