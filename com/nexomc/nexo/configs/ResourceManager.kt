package com.nexomc.nexo.configs

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.utils.NexoYaml.Companion.loadConfiguration
import com.nexomc.nexo.utils.printOnFailure
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.*

class ResourceManager(val plugin: JavaPlugin) {
    private var settings: Resource? = null
    private var mechanics: Resource? = null
    private var converter: Resource? = null
    data class Resource(val file: File, val config: YamlConfiguration)

    fun settings() = settingsEntry().config

    fun settingsEntry() = settings ?: entry("settings.yml").also { settings = it }

    fun mechanics() = mechanicsEntry().config

    fun mechanicsEntry() = mechanics ?: entry("mechanics.yml").also { mechanics = it }

    fun converter() = converter ?: entry("converter.yml").also { converter = it }

    fun entry(fileName: String) = extractConfiguration(fileName).let { Resource(it, loadConfiguration(it)) }

    fun extractConfiguration(fileName: String) =
        plugin.dataFolder.resolve(fileName).also { if (!it.exists()) runCatching { plugin.saveResource(fileName, false) }.printOnFailure() }

    fun extractConfigsInFolder(folder: String, fileExtension: String) {
        val jarEntries = NexoPlugin.jarFile?.entries() ?: return
        while (jarEntries.hasMoreElements()) {
            val jarEntry = jarEntries.nextElement()
            val isSuitable = jarEntry.name.startsWith("$folder/") && jarEntry.name.endsWith(".$fileExtension")
            if (!jarEntry.isDirectory && isSuitable) plugin.saveResource(jarEntry.name, true)
        }
    }
}
