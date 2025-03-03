package com.nexomc.nexo.commands

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.commands.LogDumpCommand.postToPasteBin
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.resolve
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.multiLiteralArgument
import gs.mclo.java.Log
import gs.mclo.java.MclogsAPI
import java.net.URI
import javax.net.ssl.HttpsURLConnection

internal fun CommandTree.dumpLogCommand() = multiLiteralArgument(nodeName = "dump_log", "dumplog") {
    withPermission("nexo.command.dumplog")
    anyExecutor { sender, _ ->
        val logfile = NexoPlugin.instance().dataFolder.absoluteFile.parentFile.parentFile.resolve("logs", "latest.log")
        val logContent = logfile.readText().replace("https://atlas\\..*\\.com:.*/".toRegex(), "[REDACTED]")
        runCatching {
            val post = MclogsAPI.share(Log(logContent))
            Logs.logSuccess("Logfile has been dumped to: " + post.url)
        }.onFailure {
            Logs.logWarn("Failed to upload logfile to mclo.gs, attempting to using pastebin - s")
            if (Settings.DEBUG.toBool()) it.printStackTrace()
            runCatching {
                Logs.logSuccess("Logfile has been dumped to: " + postToPasteBin(logContent))
            }.onFailure {
                Logs.logError("Failed to use backup solution with pastebin")
                if (Settings.DEBUG.toBool()) it.printStackTrace()
            }
        }
    }
}

object LogDumpCommand {

    internal fun postToPasteBin(text: String): String? {
        val postData = text.encodeToByteArray()
        val postDataLength = postData.size

        val requestURL = "https://hastebin.com/documents"
        val url = URI.create(requestURL).toURL()
        val conn = url.openConnection() as HttpsURLConnection
        conn.doOutput = true
        conn.instanceFollowRedirects = false
        conn.requestMethod = "POST"
        conn.setRequestProperty("User-Agent", "Hastebin Java Api")
        conn.setRequestProperty("Content-Length", postDataLength.toString())
        conn.useCaches = false


        return conn.inputStream.use {
            it.reader(Charsets.UTF_8).buffered().readLine()
        }
    }
}
