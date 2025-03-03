package com.nexomc.nexo.pack

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.FileUtils
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.printOnFailure
import com.nexomc.nexo.utils.resolve
import team.unnamed.creative.ResourcePack
import java.io.File

object NexoPackSquash {

    private val packSquashCache = NexoPlugin.instance().dataFolder.resolve("pack", "packsquash", ".cache").apply { mkdirs() }
    private var process: Process? = null
        set(value) {
            process?.destroy()
            field = value
        }

    fun stopPackSquash() {
        process?.destroy()
    }

    fun squashPack(resourcePack: ResourcePack, hash: String): ResourcePack {
        runCatching {
            if (!Settings.PACK_USE_PACKSQUASH.toBool()) return resourcePack
            val packSquashExe = File(Settings.PACKSQUASH_EXEC_PATH.toString())
            val packSquashSettings = File(Settings.PACKSQUASH_SETTINGS_PATH.toString())
            if (!packSquashSettings.exists() || !packSquashExe.exists()) return resourcePack

            FileUtils.setHidden(packSquashCache.toPath())
            val inputDirectory = packSquashCache.resolve(hash)
            val squashedZip = packSquashCache.resolve("$hash.zip")
            if (squashedZip.exists()) return PackGenerator.packReader.readFromZipFile(squashedZip)

            PackGenerator.packWriter.writeToDirectory(inputDirectory, resourcePack)
            PackGenerator.packWriter.writeToZipFile(squashedZip, resourcePack)
            val tomlContent = packSquashSettings.readText()
                .replace("pack_directory = .*".toRegex(), "pack_directory = '${inputDirectory.absolutePath()}'")
                .replace("output_file_path = .*".toRegex(), "output_file_path = '${squashedZip.absolutePath()}'")
            packSquashSettings.writeText(tomlContent)

            runCatching {
                Logs.logInfo("Squashing NexoPack using PackSquash...")
                val processBuilder = ProcessBuilder(packSquashExe.absolutePath(), packSquashSettings.absolutePath())
                processBuilder.directory(NexoPlugin.instance().dataFolder)
                processBuilder.redirectInput(ProcessBuilder.Redirect.PIPE)
                processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE)
                processBuilder.redirectErrorStream(true)
                val process = processBuilder.start()

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
            }.printOnFailure()

            inputDirectory.deleteRecursively()

            return PackGenerator.packReader.readFromZipFile(squashedZip)
        }

        return resourcePack
    }

    private fun File.absolutePath() = this.absolutePath.replace("\\", "/")

}