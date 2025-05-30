package com.nexomc.nexo.converter

import com.nexomc.nexo.NexoPlugin
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration

data class Converter(
    val oraxenConverter: OraxenConverter,
    val itemsadderConverter: ItemsAdderConverter,
    val nexoConverter: NexoConverter,
) {
    fun save() {
        runCatching {
            NexoPlugin.instance().dataFolder.resolve("converter.yml").let {
                NexoPlugin.instance().resourceManager().converter.config.also { c ->
                    c.getConfigurationSection("itemsadderConverter")?.apply {
                        set("changedItemIds", itemsadderConverter.changedItemIds)
                        set("hasBeenConverted", null)
                    }
                    c.set("oraxenConverter.hasBeenConverted", null)
                    c.set("nexoConverter.furnitureConverter", nexoConverter.furnitureConverter)
                }.save(it)
            }
        }.onFailure { it.printStackTrace() }
    }

    constructor(config: YamlConfiguration) : this(
        config.getConfigurationSection("oraxenConverter")?.let(::OraxenConverter) ?: OraxenConverter(),
        config.getConfigurationSection("itemsadderConverter")?.let(::ItemsAdderConverter) ?: ItemsAdderConverter(),
        config.getConfigurationSection("nexoConverter")?.let(::NexoConverter) ?: NexoConverter()
    )

    data class NexoConverter(val furnitureConverter: Map<String, String> = mapOf()) {
        constructor(config: ConfigurationSection) : this(
            config.getConfigurationSection("furnitureConverter")?.let { section ->
                section.getKeys(false).associateWith { section.getString(it)!! }
            } ?: mapOf()
        )
    }

    data class OraxenConverter(
        val convertItems: Boolean = true,
        val convertResourcePack: Boolean = true,
        val convertSettings: Boolean = true,
        val convertFurnitureOnLoad: Boolean = true
    ) {
        constructor(oraxenConverter: ConfigurationSection) : this(
            oraxenConverter.getBoolean("convertItems"),
            oraxenConverter.getBoolean("convertResourcePack"),
            oraxenConverter.getBoolean("convertSettings"),
            oraxenConverter.getBoolean("convertFurnitureOnLoad")
        )
    }

    data class ItemsAdderConverter(
        val convertItems: Boolean = true,
        val convertResourcePack: Boolean = true,
        val convertSettings: Boolean = true,
        val convertFurnitureOnLoad: Boolean = true,

        val changedItemIds: MutableMap<String, String> = mutableMapOf()
    ) {
        constructor(config: ConfigurationSection) : this(
            config.getBoolean("convertItems"),
            config.getBoolean("convertResourcePack"),
            config.getBoolean("convertSettings"),
            config.getBoolean("convertFurnitureOnLoad"),
            config.getConfigurationSection("changedItemIds")?.let {
                it.getKeys(false).associateWith { s -> it.getString(s)!! }.toMutableMap()
            } ?: mutableMapOf()
        )
    }
}