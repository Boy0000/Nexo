package com.nexomc.nexo.configs

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.utils.NexoYaml.Companion.loadConfiguration
import com.nexomc.nexo.utils.printOnFailure
import java.io.File
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin

class ResourceManager(val plugin: JavaPlugin) {
    val settings: Resource by lazy { entry("settings.yml") }
    val mechanics: Resource by lazy { entry("mechanics.yml") }
    val converter: Resource by lazy { entry("converter.yml") }
    data class Resource(val file: File, val config: YamlConfiguration) {
        constructor(file: File) : this(file, loadConfiguration(file))
    }

    private fun entry(fileName: String) = Resource(extractConfiguration(fileName))

    fun extractConfiguration(fileName: String) =
        plugin.dataFolder.resolve(fileName).also { if (!it.exists()) runCatching { plugin.saveResource(fileName, false) }.printOnFailure() }

    fun extractConfigsInFolder(folder: String, fileExtension: String) {
        val jarEntries = NexoPlugin.jarFile?.entries() ?: return
        jarEntries.asSequence().forEach { entry ->
            if (!entry.isDirectory || !entry.name.startsWith("$folder/") || !entry.name.endsWith(".$fileExtension")) return@forEach
            plugin.saveResource(entry.name, true)
        }
    }
}
