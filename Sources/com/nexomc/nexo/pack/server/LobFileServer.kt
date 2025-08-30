package com.nexomc.nexo.pack.server

import com.google.gson.JsonParser
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.SchedulerUtils
import com.nexomc.nexo.utils.logs.Logs
import kotlinx.coroutines.Job
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.EntityUtils
import java.util.*

class LobFileServer : NexoPackServer {
    private var packUrl: String? = null
    private var hash: String? = null
    private var packUUID: UUID? = null
    private var uploadFuture: Job? = null

    override fun uploadPack(): Job {
        if (hash != NexoPlugin.instance().packGenerator().builtPack()!!.hash()) {
            uploadFuture?.cancel()
            uploadFuture = null
        }

        if (uploadFuture == null) uploadFuture = SchedulerUtils.launch {
            runCatching {
                HttpClients.createDefault().use { httpClient ->
                    val request = HttpPost("https://lobfile.com/api/v3/upload")
                    val packData = NexoPlugin.instance().packGenerator().builtPack()!!.data().toByteArray()
                    val hash = NexoPlugin.instance().packGenerator().builtPack()!!.hash()

                    val httpEntity = MultipartEntityBuilder.create()
                        .addBinaryBody("file", packData, ContentType.DEFAULT_BINARY, "pack.zip") // Use correct field name
                        .addTextBody("sha_1", hash) // Optional
                        .build()

                    request.entity = httpEntity
                    request.setHeader("X-API-Key", Settings.LOBFILE_API_KEY.toString())

                    val response = httpClient.execute(request)
                    val responseString = EntityUtils.toString(response.entity)
                    val jsonOutput = runCatching {
                        JsonParser.parseString(responseString).asJsonObject
                    }.onFailure {
                        Logs.logError("Malformed response from LobFile.")
                        if (Settings.DEBUG.toBool()) {
                            Logs.logWarn(responseString)
                            it.printStackTrace()
                        } else Logs.logWarn(it.message!!)
                    }.getOrNull() ?: return@launch

                    if (jsonOutput.has("success") && jsonOutput["success"].asBoolean) {
                        packUrl = jsonOutput["url"].asString
                        this@LobFileServer.hash = hash
                        packUUID = UUID.nameUUIDFromBytes(hash.toByteArray())

                        Logs.logSuccess("ResourcePack uploaded: $packUrl")
                        return@launch
                    }

                    Logs.logError("Upload failed: " + jsonOutput.get("error")?.asString)
                }
            }.onFailure {
                Logs.logError("ResourcePack upload to LobFile failed.")
                if (Settings.DEBUG.toBool()) it.printStackTrace()
                else Logs.logWarn(it.message!!)
            }
        }

        return uploadFuture!!
    }


    override fun packUrl() = packUrl ?: ""

    override val isPackUploaded: Boolean
        get() = NexoPlugin.instance().packGenerator().packGenJob?.isCompleted != false && uploadFuture?.isCompleted != false
}