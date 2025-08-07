package com.nexomc.nexo.configs

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.utils.NexoYaml.Companion.loadConfiguration
import com.nexomc.nexo.utils.printOnFailure
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class ResourceManager(val plugin: JavaPlugin) {
    val settings: Resource by lazy { Resource(extractConfiguration("settings.yml")) }
    val mechanics: Resource by lazy { Resource(extractConfiguration("mechanics.yml")) }
    val converter: Resource by lazy { Resource(extractConfiguration("converter.yml")) }
    data class Resource(val file: File, val config: YamlConfiguration) {
        constructor(file: File) : this(file, loadConfiguration(file))
    }

    fun extractConfiguration(fileName: String): File {
        return plugin.dataFolder.resolve(fileName).also {
            if (!it.exists()) runCatching { plugin.saveResource(fileName, false) }.printOnFailure()
        }
    }

    fun extractConfigsInFolder(folder: String, fileExtension: String) {
        val jarEntries = NexoPlugin.jarFile?.entries() ?: return
        jarEntries.asSequence().forEach { entry ->
            if (entry.isDirectory || !entry.name.startsWith("$folder/") || !entry.name.endsWith(".$fileExtension")) return@forEach
            plugin.saveResource(entry.name, true)
        }
    }
}
