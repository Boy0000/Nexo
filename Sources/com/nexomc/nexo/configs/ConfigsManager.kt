package com.nexomc.nexo.configs

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.compatibilities.mmoitems.WrappedMMOItem
import com.nexomc.nexo.compatibilities.mythiccrucible.WrappedCrucibleItem
import com.nexomc.nexo.converter.NexoConverter
import com.nexomc.nexo.glyphs.AnimatedGlyph
import com.nexomc.nexo.glyphs.Glyph
import com.nexomc.nexo.glyphs.ReferenceGlyph
import com.nexomc.nexo.glyphs.RequiredGlyph
import com.nexomc.nexo.items.CustomModelData
import com.nexomc.nexo.items.ItemBuilder
import com.nexomc.nexo.items.ItemParser
import com.nexomc.nexo.items.ItemTemplate
import com.nexomc.nexo.mechanics.custom_block.CustomBlockRegistry
import com.nexomc.nexo.utils.AdventureUtils
import com.nexomc.nexo.utils.AdventureUtils.tagResolver
import com.nexomc.nexo.utils.KeyUtils
import com.nexomc.nexo.utils.NexoYaml
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.associateFastLinked
import com.nexomc.nexo.utils.associateFastLinkedWith
import com.nexomc.nexo.utils.childSections
import com.nexomc.nexo.utils.getKey
import com.nexomc.nexo.utils.getStringListOrNull
import com.nexomc.nexo.utils.getStringOrNull
import com.nexomc.nexo.utils.listYamlFiles
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.printOnFailure
import com.nexomc.nexo.utils.rename
import com.nexomc.nexo.utils.toIntRangeOrNull
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class ConfigsManager(private val plugin: JavaPlugin) {
    private var settings: YamlConfiguration? = null
    var mechanics: YamlConfiguration = defaultMechanics; private set
    var sounds: YamlConfiguration = defaultSounds; private set
    var languages: YamlConfiguration = defaultLanguages; private set
    var messages: YamlConfiguration = defaultMessages; private set

    fun settings(): YamlConfiguration {
        if (settings == null) settings = Settings.validateSettings()
        return settings!!
    }

    fun validatesConfig() {
        val resourceManager = NexoPlugin.instance().resourceManager()
        settings = Settings.validateSettings()
        mechanics = validate(resourceManager, "mechanics.yml", defaultMechanics)
        sounds = validate(resourceManager, "sounds.yml", defaultSounds)
        languages = validate(resourceManager, "languages.yml", defaultLanguages)
        val messagesFolder = plugin.dataFolder.resolve("messages").apply { mkdirs() }
        plugin.dataFolder.resolve("languages").takeIf { it.exists() && it.isDirectory }?.renameTo(messagesFolder)
        messages = validate(resourceManager, "messages/${Settings.PLUGIN_LANGUAGE}.yml", defaultMessages)
        AdventureUtils.reload()

        if (itemsFolder.list().isNullOrEmpty() && Settings.GENERATE_DEFAULT_CONFIGS.toBool()) resourceManager.extractConfigsInFolder("items", "yml")

        if (glyphsFolder.list().isNullOrEmpty()) {
            if (Settings.GENERATE_DEFAULT_CONFIGS.toBool()) resourceManager.extractConfigsInFolder("glyphs", "yml")
            else resourceManager.extractConfiguration("glyphs/nexo_defaults/interface.yml")
        }

        if (!schematicsFolder.exists() && Settings.GENERATE_DEFAULT_CONFIGS.toBool())
            resourceManager.extractConfigsInFolder("schematics", "schem")
    }

    private fun validate(
        resourcesManager: ResourceManager,
        configName: String,
        defaultConfiguration: YamlConfiguration
    ): YamlConfiguration {
        val configurationFile = resourcesManager.extractConfiguration(configName)
        val configuration = NexoYaml.loadConfiguration(configurationFile)
        var updated = false

        defaultConfiguration.getKeys(true).forEach { key: String ->
            if (key.startsWith(Settings.NEXO_INV_LAYOUT.path) || configuration.get(key) != null) return@forEach
            updated = true
            Message.UPDATING_CONFIG.log(tagResolver("option", key))
            configuration.set(key, defaultConfiguration.get(key))
        }

        configuration.getKeys(true).filter { it in removedYamlKeys }.forEach { key: String ->
            updated = true
            Message.REMOVING_CONFIG.log(tagResolver("option", key))
            configuration.set(key, null)
        }

        configuration.getKeys(false).associateWith { movedYamlKeys[it] }.forEach { old, new ->
            if (new == null) return@forEach
            updated = true
            Message.UPDATING_CONFIG.log(tagResolver("option", new))
            configuration.set(new, configuration.get(old))
            configuration.set(old, null)
        }

        runCatching {
            if (updated) configuration.save(configurationFile)
        }.onFailure {
            Logs.logError("Failed to save updated configuration file: ${configurationFile.name}")
            if (Settings.DEBUG.toBool()) it.printStackTrace()
        }
        return configuration
    }

    fun parseGlyphConfigs(): Collection<Glyph> {
        val output = mutableListOf<Glyph>()
        val referenceGlyphs = mutableMapOf<String, ConfigurationSection>()
        val gifGlyphs = mutableMapOf<String, ConfigurationSection>()
        Glyph.assignedGlyphUnicodes.clear()

        glyphFiles().onEach(NexoConverter::processGlyphConfigs).associateWith(NexoYaml::loadConfiguration).apply {
            entries.flatMap { it.value.childSections().entries }.forEach { (glyphId, glyphSection) ->
                // Reference glyphs do not contain unicodes as they link to a normal glyph only
                if ("reference" in glyphSection) referenceGlyphs[glyphId] = glyphSection
                if ("gif" in glyphSection) gifGlyphs[glyphId] = glyphSection
                else Glyph.assignedGlyphUnicodes[glyphId] = Glyph.definedUnicodes(glyphSection) ?: return@forEach
            }
        }.onEach { (file, configuration) ->
            var fileChanged = false

            configuration.childSections().entries.onEach { (glyphId, glyphSection) ->
                // Skip reference-glyphs until all normal glyphs are parsed
                if (glyphId in referenceGlyphs || glyphId in gifGlyphs) return@onEach

                runCatching {
                    if (!fileChanged || glyphId !in Glyph.assignedGlyphUnicodes) fileChanged = true
                    output += Glyph(glyphSection)
                }.onFailure {
                    Logs.logWarn("Failed to load glyph $glyphId")
                    if (Settings.DEBUG.toBool()) it.printStackTrace()
                }
            }.onEach { (referenceId, _) ->
                val referenceSection = referenceGlyphs[referenceId] ?: return@onEach
                val index = referenceSection.getString("index")?.toIntRangeOrNull() ?: return@onEach
                val glyphId = referenceSection.getString("reference") ?: return@onEach
                val glyph = output.find { it.id == glyphId } ?: return@onEach Logs.logError("Reference-Glyph $referenceId tried referencing a Glyph $glyphId, but it does not exist...")
                val permission = referenceSection.getString("permission") ?: glyph.permission
                val placeholders = referenceSection.getStringListOrNull("placeholders") ?: glyph.placeholders

                if (glyph.unicodes.joinToString("").count() < index.last || index.first <= 0) {
                    val i = if (index.count() == 1) index.first else index
                    return@onEach Logs.logError("Reference-Glyph $referenceId used invalid index $i, $glyphId has indexes $index")
                }

                runCatching {
                    output += ReferenceGlyph(glyph, referenceId, index, permission, placeholders)
                }.onFailure {
                    Logs.logWarn("Failed to load Reference Glyph $glyphId")
                    if (Settings.DEBUG.toBool()) it.printStackTrace()
                }
            }.onEach { (gifId, _) ->
                runCatching {
                    output += AnimatedGlyph(gifGlyphs[gifId] ?: return@onEach)
                }.onFailure {
                    Logs.logWarn("Failed to load AnimatedGlyph $gifId")
                    if (Settings.DEBUG.toBool()) it.printStackTrace()
                }
            }

            if (fileChanged && !Settings.DISABLE_AUTOMATIC_GLYPH_CODE.toBool()) runCatching {
                configuration.save(file)
            }.onFailure {
                Logs.logWarn("Failed to save updated glyph file: ${file.name}")
                if (Settings.DEBUG.toBool()) it.printStackTrace()
            }
        }

        runCatching {
            output += RequiredGlyph
        }.printOnFailure()

        return output
    }

    internal var itemConfigs: Map<File, YamlConfiguration> = mapOf()
        get() {
            if (field.isEmpty()) field = itemFiles().associateFastLinkedWith(NexoYaml::loadConfiguration)
            return field
        }

    internal fun parseItemConfig() = itemConfigs.entries.associateFastLinked { it.key to parseItemConfig(it) }

    fun assignAllUsedCustomVariations() {
        itemConfigs.forEach { file, config ->
            config.childSections().forEach { (itemId, section) ->
                val blockSection = section.getConfigurationSection("Mechanics.custom_block") ?: return@forEach
                val blockType = blockSection.getStringOrNull("type") ?: return@forEach
                val customVariation = section.getInt("Mechanics.custom_block.custom_variation").takeIf { it > 0 } ?: return@forEach
                val model = section.getKey("Mechanics.custom_block.model") ?: section.getKey("Pack.model") ?: Key.key(itemId)

                val existingBlock = CustomBlockRegistry.DATAS[blockType]?.object2IntEntrySet()?.find { it.intValue == customVariation && it.key != model }
                when (existingBlock) {
                    null -> CustomBlockRegistry.DATAS.getOrPut(blockType, ::Object2IntLinkedOpenHashMap)[model] = customVariation
                    else -> Logs.logError("<red>$itemId</red> in <red>${file.path}</red> is using CustomVariation <yellow>$customVariation</yellow>, which is already assigned to <red>$existingBlock")
                }
            }
        }
    }

    fun assignAllUsedCustomModelDatas() {
        itemConfigs.forEach { file, config ->
            config.childSections().forEach { (itemId, itemSection) ->
                val packSection = itemSection.getConfigurationSection("Pack") ?: return@forEach
                val material = Material.getMaterial(itemSection.getString("material", "")!!)
                    ?: WrappedCrucibleItem(itemSection).material
                    ?: WrappedMMOItem(itemSection, true).material
                    ?: Material.PAPER

                validatePackSection(itemId, packSection)
                val modelData = packSection.getInt("custom_model_data", -1).takeUnless { it == -1 } ?: return@forEach
                val model = (packSection.getString("model")?.takeUnless(String::isNullOrEmpty) ?: itemId).takeIf(Key::parseable)?.let(Key::key) ?: KeyUtils.MALFORMED_KEY_PLACEHOLDER

                val existingModel = CustomModelData.DATAS[material]?.object2IntEntrySet()?.find { it.intValue == modelData && it.key != model }?.key?.key()?.asString()
                when (existingModel) {
                    null -> CustomModelData.DATAS.getOrPut(material, ::Object2IntLinkedOpenHashMap)[model] = modelData
                    else -> Logs.logError("<red>$itemId</red> in <red>${file.path}</red> is using CustomModelData <yellow>$modelData</yellow>, which is already assigned to <red>$existingModel")
                }
            }
        }
    }

    fun parseAllItemTemplates() {
        ItemTemplate.itemTemplates.clear()
        val templateIds = mutableListOf<String>()
        val itemConfigs = itemConfigs.values.flatMap { it.childSections().toList() }.onEach { (_, section) ->
            section.getStringOrNull("template")?.let(templateIds::add)
            section.getStringListOrNull("templates")?.let(templateIds::addAll)
        }.toMap()

        templateIds.forEach { templateId ->
            itemConfigs[templateId]?.let(ItemTemplate::register)
        }
    }

    private fun validatePackSection(itemId: String, packSection: ConfigurationSection) {
        val model = packSection.getString("model")?.takeUnless { it.isEmpty() } ?: itemId

        if (!Key.parseable(model)) {
            Logs.logWarn("Found invalid model in NexoItem <blue>$itemId</blue>: <aqua>$model")
            Logs.logWarn("Model-paths must only contain characters <yellow>[a-z0-9/._-]")
        }

        packSection.getStringList("textures").forEach { texture: String ->
            if (Key.parseable(texture)) return@forEach
            Logs.logWarn("Found invalid texture in NexoItem <blue>$itemId</blue>: <aqua>$texture")
            Logs.logWarn("Texture-paths must only contain characters <yellow>[a-z0-9/._-]")
        }
    }

    val ERROR_ITEM by lazy {
        runCatching {
            ItemParser(Settings.ERROR_ITEM.toConfigSection()!!).buildItem()
        }.getOrDefault(ItemBuilder(Material.PODZOL))
    }

    private fun parseItemConfig(entry: Map.Entry<File, YamlConfiguration>) = parseItemConfig(entry.key, entry.value)
    private fun parseItemConfig(itemFile: File, config: YamlConfiguration): Object2ObjectLinkedOpenHashMap<String, ItemBuilder> {
        val parseMap = Object2ObjectLinkedOpenHashMap<String, ItemParser>()
        var configUpdated = false

        val copy = YamlConfiguration().also { NexoYaml.copyConfigurationSection(config, it) }
        NexoConverter.processItemConfigs(config)
        if (!NexoYaml.equals(copy, config)) configUpdated = true

        config.childSections().forEach { itemId, section ->
            if (!ItemTemplate.isTemplate(itemId)) parseMap[itemId] = ItemParser(section)
        }

        val map = Object2ObjectLinkedOpenHashMap<String, ItemBuilder>()
        parseMap.forEach { (itemId, itemParser) ->
            map[itemId] = runCatching {
                itemParser.buildItem()
            }.onFailure {
                Logs.logError("ERROR BUILDING ITEM \"$itemId\" from file ${itemFile.path}")
                if (Settings.DEBUG.toBool()) it.printStackTrace()
                else it.message?.let(Logs::logWarn)
            }.getOrNull() ?: ERROR_ITEM.itemName(Component.text(itemId))
            if (itemParser.isConfigUpdated) configUpdated = true
        }

        if (configUpdated) runCatching {
            config.childSections().forEach { _, section ->
                when {
                    VersionUtil.atleast("1.20.5") -> section.rename("displayname", "itemname")
                    else -> section.rename("itemname", "dispayname")
                }
                section.getMapList("AttributeModifiers").takeUnless { it.isEmpty() }?.map {
                    it.remove("key")
                    it.remove("name")
                    it.remove("uuid")
                }
            }
            config.save(itemFile)
        }.printOnFailure(true)

        return map
    }

    private fun itemFiles(): List<File> = itemsFolder.listYamlFiles(true).filter(NexoYaml::isValidYaml).sortedBy(File::nameWithoutExtension)

    private fun glyphFiles(): List<File> = glyphsFolder.listYamlFiles(true).filter(NexoYaml::isValidYaml).sortedBy(File::nameWithoutExtension)

    companion object {
        private val defaultMechanics: YamlConfiguration = extractDefault("mechanics.yml")
        private val defaultSounds: YamlConfiguration = extractDefault("sounds.yml")
        private val defaultLanguages: YamlConfiguration = extractDefault("languages.yml")
        private val defaultMessages: YamlConfiguration = extractDefault("messages/english.yml")
        private val itemsFolder: File = NexoPlugin.instance().dataFolder.resolve("items")
        private val glyphsFolder: File = NexoPlugin.instance().dataFolder.resolve("glyphs")
        val schematicsFolder: File = NexoPlugin.instance().dataFolder.resolve("schematics")
        val settingsFile = NexoPlugin.instance().dataFolder.resolve("settings.yml")

        private fun extractDefault(source: String): YamlConfiguration {
            return NexoPlugin.instance().getResource(source)?.bufferedReader()?.use {
                runCatching {
                    YamlConfiguration.loadConfiguration(it)
                }.onFailure {
                    Logs.logError("Failed to extract default file: $source")
                    if (Settings.DEBUG.toBool()) it.printStackTrace()
                }.getOrNull()
            } ?: YamlConfiguration()
        }

        private val movedYamlKeys = mapOf(
            "noteblock" to "custom_blocks.noteblock",
            "stringblock" to "custom_blocks.stringblock",
            "chorusblock" to "custom_blocks.chorusblock",
        )

        private val removedYamlKeys = listOf(
            "armorpotioneffects",
            "consumable_potion_effects",
            "consumable",
            "music_disc",
            "durability",
            "efficiency",
            "aura",
            "hat",
            "skin",
            "skinnable",
            "lifeleech",
            "bigmining",
            "bottledexp",
            "bedrockbreak",
            "watering",
            "block",
            "repair.oraxen_durability_only",
            "noteblock.custom_block_sounds",
            "stringblock.custom_block_sounds",
            "chorusblock.custom_block_sounds",
            "noteblock.tool_types",
            "stringblock.tool_types",
            "chorusblock.tool_types",
            "detect_viabackwards"
        )
    }
}
