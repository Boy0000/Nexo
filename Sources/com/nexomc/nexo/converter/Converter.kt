package com.nexomc.nexo.converter

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.utils.NexoYaml
import com.nexomc.nexo.utils.ensureCast
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.printOnFailure
import com.nexomc.nexo.utils.safeCast
import net.kyori.adventure.key.Key
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemorySection
import org.bukkit.configuration.file.YamlConfiguration
import org.spongepowered.configurate.yaml.internal.snakeyaml.Yaml

data class Converter(
    val oraxenConverter: OraxenConverter,
    val itemsadderConverter: ItemsAdderConverter,
) {
    fun save() {
        runCatching {
            NexoPlugin.instance().dataFolder.resolve("converter.yml").let {
                NexoPlugin.instance().resourceManager().converter().config.also { c ->
                    c.getConfigurationSection("oraxenConverter")?.set("hasBeenConverted", oraxenConverter.hasBeenConverted)
                    c.getConfigurationSection("itemsadderConverter")?.set("hasBeenConverted", itemsadderConverter.hasBeenConverted)
                    c.getConfigurationSection("itemsadderConverter")?.set("changedItemIds", itemsadderConverter.changedItemIds)
                }.save(it)
            }
        }.onFailure { it.printStackTrace() }
    }

    constructor(config: YamlConfiguration) : this(
        config.getConfigurationSection("oraxenConverter")?.let(::OraxenConverter) ?: OraxenConverter(),
        config.getConfigurationSection("itemsadderConverter")?.let(::ItemsAdderConverter) ?: ItemsAdderConverter(),
    )

    data class OraxenConverter(
        val convertItems: Boolean = true,
        val convertResourcePack: Boolean = true,
        val convertSettings: Boolean = true,
        val convertFurnitureOnLoad: Boolean = true,

        var hasBeenConverted: Boolean = false,
    ) {
        constructor(oraxenConverter: ConfigurationSection) : this(
            oraxenConverter.getBoolean("convertItems"),
            oraxenConverter.getBoolean("convertResourcePack"),
            oraxenConverter.getBoolean("convertSettings"),
            oraxenConverter.getBoolean("convertFurnitureOnLoad"),
            oraxenConverter.getBoolean("hasBeenConverted", true)
        )
    }

    data class ItemsAdderConverter(
        val convertItems: Boolean = true,
        val convertResourcePack: Boolean = true,
        val convertSettings: Boolean = true,
        val convertFurnitureOnLoad: Boolean = true,

        var hasBeenConverted: Boolean = false,
        val changedItemIds: MutableMap<String, String> = mutableMapOf()
    ) {
        constructor(config: ConfigurationSection) : this(
            config.getBoolean("convertItems"),
            config.getBoolean("convertResourcePack"),
            config.getBoolean("convertSettings"),
            config.getBoolean("convertFurnitureOnLoad"),
            config.getBoolean("hasBeenConverted", true),
            config.getConfigurationSection("changedItemIds")?.let {
                it.getKeys(false).associateWith { s -> it.getString(s)!! }.toMutableMap()
            } ?: mutableMapOf()
        )
    }
}