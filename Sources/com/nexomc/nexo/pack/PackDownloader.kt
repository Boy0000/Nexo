package com.nexomc.nexo.pack

import com.google.gson.JsonParser
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.FileUtils.setHidden
import com.nexomc.nexo.utils.SchedulerUtils
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.resolve
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.apache.commons.lang3.StringUtils
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Duration.Companion.seconds

class PackDownloader {
    @Volatile
    private var requiredPackDownload: Job? = null

    @Volatile
    private var defaultPackDownload: Job? = null

    fun downloadRequiredPack(): Job {
        if (requiredPackDownload?.isCompleted == false) return requiredPackDownload!!

        requiredPackDownload = SchedulerUtils.launch(Dispatchers.IO) {
            if (VersionUtil.isLeaked) return@launch
            runCatching {
                val hash = checkPackHash("RequiredResourcePack") ?: return@launch
                val zipPath = PackGenerator.externalPacks.resolve("RequiredPack_$hash.zip")
                removeOldHashPack("RequiredPack", hash)
                if (!zipPath.exists() && !zipPath.resolveSibling("RequiredPack_$hash").exists()) {
                    downloadPack("RequiredResourcePack", zipPath.toPath())
                    delay(2.seconds)
                }
            }.onFailure {
                Logs.logWarn("Failed to download RequiredPack...")
                if (Settings.DEBUG.toBool()) it.printStackTrace()
            }
        }

        return requiredPackDownload!!
    }

    fun downloadDefaultPack(): Job {
        if (defaultPackDownload?.isCompleted == false) return defaultPackDownload!!

        defaultPackDownload = SchedulerUtils.launch(Dispatchers.IO) {
            if (VersionUtil.isLeaked) return@launch
            if (!Settings.PACK_IMPORT_DEFAULT.toBool()) return@launch
            if (VersionUtil.isCompiled || VersionUtil.isCI || VersionUtil.isLeaked) return@launch logSkipReason()

            runCatching {
                val hash = checkPackHash("DefaultResourcePack") ?: return@launch
                val zipPath = PackGenerator.externalPacks.resolve("DefaultPack_$hash.zip")
                removeOldHashPack("DefaultPack", hash)
                if (!zipPath.exists() && !zipPath.resolveSibling("DefaultPack_$hash").exists()) {
                    Logs.logInfo("Downloading default resourcepack...")
                    downloadPack("DefaultResourcePack", zipPath.toPath())
                    delay(2.seconds)
                } else Logs.logSuccess("Skipped downloading DefaultPack as it is up to date!")
            }.onFailure {
                Logs.logWarn("Failed to download DefaultPack")
                if (Settings.DEBUG.toBool()) it.printStackTrace()
            }
        }

        return defaultPackDownload!!
    }

    private fun logSkipReason() {
        when {
            VersionUtil.isCompiled -> Logs.logWarn("Skipping download of Nexo pack, compiled versions do not include assets")
            VersionUtil.isCI -> Logs.logWarn("Skipping download of Nexo pack, CI versions do not include assets")
            VersionUtil.isLeaked -> Logs.logError("Skipping download of Nexo pack, pirated versions do not include assets")
        }
    }

    companion object {
        @OptIn(ExperimentalEncodingApi::class)
        private fun readToken(): String? {
            if (VersionUtil.isLeaked || VersionUtil.isCompiled || VersionUtil.isCI) return null
            return (
                    NexoPlugin.instance().dataFolder.resolve("token.secret").takeIf { it.exists() }?.inputStream()
                        ?: NexoPlugin.instance().getResource("token.secret")
                    )?.let { accessStream ->
                    runCatching {
                        InputStreamReader(accessStream).use { reader ->
                            val token = YamlConfiguration.loadConfiguration(reader)
                            Base64.Mime.decode(token.getString("token1", "").plus(token.getString("token2", "")!!)).decodeToString()
                        }
                    }.onFailure { exception ->
                        logTokenError(exception)
                    }.getOrNull()
                }.also {
                    if (it == null) Logs.logWarn("Missing token-file, please contact the developer!")
                }
        }

        private fun logTokenError(exception: Throwable) {
            if (Settings.DEBUG.toBool()) exception.printStackTrace()
            Logs.logWarn(exception.message ?: "An unexpected error occurred while reading token.")
            Logs.logError("Failed to read token-file. Please contact the developer!", true)
        }

        private fun removeOldHashPack(filePrefix: String, newHash: String?) {
            PackGenerator.externalPacks.listFiles()?.filter { file ->
                file.name.startsWith(filePrefix) && !file.name.endsWith("$newHash.zip")
            }?.forEach(File::delete)
        }

        private fun downloadPack(repo: String, zipPath: Path) {
            val token = readToken() ?: return
            val connection = runCatching {
                val url = URI.create("https://api.github.com/repos/Nexo-MC/$repo/zipball/master").toURL()
                (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setCommonHeaders(token)
                }
            }.getOrNull() ?: return Logs.logError(
                "Failed to establish a connection to download $repo pack. Please contact the developer!",
                true
            )

            runCatching {
                connection.inputStream.use { input ->
                    unzipToPath(input, zipPath)
                }
            }.onFailure { exception ->
                if (Settings.DEBUG.toBool()) exception.printStackTrace()
                Logs.logError("Failed to download $repo pack. Please contact the developer!", true)
            }.also {
                connection.disconnect() // Ensure the connection is closed after usage
            }
        }


        private fun HttpURLConnection.setCommonHeaders(token: String) {
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }

        private fun unzipToPath(inputStream: java.io.InputStream, zipPath: Path) {
            ZipInputStream(inputStream).use { zis ->
                FileOutputStream(zipPath.toString()).use { fos ->
                    ZipOutputStream(fos).use { zos ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            if (!entry.isDirectory) {
                                zos.putNextEntry(ZipEntry(StringUtils.substringAfter(entry.name, "/")))
                                zis.copyTo(zos, bufferSize = 1024)
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                }
                setHidden(zipPath)
            }
        }

        private fun checkPackHash(repo: String): String? {
            val token = readToken() ?: return null
            return runCatching {
                val url = URI.create("https://api.github.com/repos/Nexo-MC/$repo/commits").toURL()
                (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setCommonHeaders(token)
                }.let { connection ->
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
                    InputStreamReader(connection.inputStream).use { reader ->
                        JsonParser.parseReader(reader).asJsonArray[0].asJsonObject["sha"].asString
                    }
                }
            }.onFailure { exception ->
                if (Settings.DEBUG.toBool()) exception.printStackTrace()
                Logs.logWarn(exception.message ?: "Failed to fetch pack hash.")
            }.getOrNull()
        }
    }
}
