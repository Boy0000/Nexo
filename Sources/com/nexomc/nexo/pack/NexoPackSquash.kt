package com.nexomc.nexo.pack

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.FileUtils
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.resolve
import java.io.File
import team.unnamed.creative.ResourcePack

class NexoPackSquash(private val resourcePack: ResourcePack) {

    companion object {
        private var process: Process? = null
            set(value) {
                process?.destroy()
                field = value
            }

        fun stopPackSquash() {
            process?.destroy()
        }
    }

    val packSquashCache = NexoPlugin.instance().dataFolder.resolve("pack", "packsquash", ".cache").apply { mkdirs() }

    fun squashPack(hash: String): Boolean {
        val inputDirectory = packSquashCache.resolve(hash)
        val squashedZip = packSquashCache.resolve("$hash.zip")

        return runCatching {
            val packSquashExe = File(Settings.PACKSQUASH_EXEC_PATH.toString()).apply { setExecutable(true) }
            val packSquashSettings = File(Settings.PACKSQUASH_SETTINGS_PATH.toString())
            if (!packSquashSettings.exists() || !packSquashExe.exists()) return false

            FileUtils.setHidden(packSquashCache.toPath())

            if (!squashedZip.exists()) {
                PackGenerator.packWriter.writeToDirectory(inputDirectory, resourcePack)
                PackGenerator.packWriter.writeToZipFile(squashedZip, resourcePack)
                val tomlContent = packSquashSettings.readText()
                    .replace("pack_directory = .*".toRegex(), "pack_directory = '${inputDirectory.absolutePath()}'")
                    .replace("output_file_path = .*".toRegex(), "output_file_path = '${squashedZip.absolutePath()}'")
                packSquashSettings.writeText(tomlContent)

                Logs.logInfo("Squashing NexoPack using PackSquash...")
                val process = ProcessBuilder(packSquashExe.absolutePath(), packSquashSettings.absolutePath())
                    .directory(NexoPlugin.instance().dataFolder).redirectInput(ProcessBuilder.Redirect.PIPE)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE).redirectErrorStream(true).start()

                process.inputReader().readLines().forEach { line ->
                    when {
                        line.isEmpty() -> return@forEach
                        line.startsWith("!") -> Logs.logError(line)
                        line.startsWith("*") -> Logs.logWarn(line)
                        line.startsWith("-") -> Logs.logInfo(line)
                        line.startsWith("#") -> Logs.logSuccess(line)
                        Settings.DEBUG.toBool() -> Logs.logInfo(line)
                    }
                }
            }
        }.onFailure {
            Logs.logError("Failed to squash pack with PackSquash: ${it.message}")
            if (Settings.DEBUG.toBool()) it.printStackTrace()
            squashedZip.delete()
        }.apply { inputDirectory.deleteRecursively() }.isSuccess
    }

    private fun File.absolutePath() = this.absolutePath.replace("\\", "/")

}