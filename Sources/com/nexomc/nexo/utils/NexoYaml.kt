package com.nexomc.nexo.utils

import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.logs.Logs
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class NexoYaml : YamlConfiguration() {
    override fun load(file: File) {
        runCatching {
            super.load(file)
        }.onFailure {
            Logs.logError("Error loading YAML configuration file: " + file.name)
            if (Settings.DEBUG.toBool()) it.printStackTrace()
        }
    }

    companion object {
        fun isValidYaml(file: File) = runCatching {
            YamlConfiguration().load(file)
        }.onFailure {
            Logs.logError("Error loading YAML configuration file: " + file.path)
            Logs.logError("Ensure that your config is formatted correctly:")
            if (Settings.DEBUG.toBool()) it.printStackTrace()
            else it.message?.let(Logs::logWarn)
        }.getOrNull() != null

        fun loadConfiguration(file: File) = runCatching {
            YamlConfiguration().apply { load(file) }
        }.onFailure {
            Logs.logError("Error loading YAML configuration file: " + file.name)
            Logs.logError("Ensure that your config is formatted correctly:")
            if (Settings.DEBUG.toBool()) it.printStackTrace()
            else it.message?.let(Logs::logWarn)
        }.getOrNull() ?: YamlConfiguration()

        fun saveConfig(file: File, section: ConfigurationSection) {
            runCatching {
                val config = loadConfiguration(file)
                config[section.currentPath!!] = section
                config.save(file)
            }.onFailure {
                Logs.logError("Error saving YAML configuration file: " + file.name)
                if (Settings.DEBUG.toBool()) it.printStackTrace()
                else it.message?.let(Logs::logWarn)
            }
        }

        fun copyConfigurationSection(source: ConfigurationSection, target: ConfigurationSection) {
            source.getKeys(false).forEach { key ->
                val (sourceValue, targetValue) = source[key] to target[key]

                if (sourceValue is ConfigurationSection) {
                    val targetSection = targetValue as? ConfigurationSection ?: target.createSection(key)
                    copyConfigurationSection(sourceValue, targetSection)
                } else target[key] = sourceValue
            }
        }
    }
}
