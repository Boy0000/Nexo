package com.nexomc.nexo.configs

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.pack.PackObfuscator
import com.nexomc.nexo.utils.AdventureUtils
import com.nexomc.nexo.utils.EnumUtils.toEnumOrElse
import com.nexomc.nexo.utils.NexoYaml.Companion.loadConfiguration
import com.nexomc.nexo.utils.PluginUtils
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.customarmor.CustomArmorType
import com.nexomc.nexo.utils.logs.Logs
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.apache.commons.lang3.EnumUtils
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import team.unnamed.creative.font.Font

enum class Settings {
    // Generic Plugin stuff
    DEBUG("debug", false, "test"),
    PLUGIN_LANGUAGE("Plugin.language", "english"),
    KEEP_UP_TO_DATE("Plugin.keep_this_up_to_date", true),
    FORMAT_ANVIL("Plugin.formatting.anvil", true),
    FORMAT_SIGNS("Plugin.formatting.signs", true),
    FORMAT_CHAT("Plugin.formatting.chat", true),
    FORMAT_BOOKS("Plugin.formatting.books", true),
    FORMAT_TABLIST("Plugin.formatting.tablist", true),
    FORMAT_BOSSBAR("Plugin.formatting.bossbar", PluginUtils.isMythicHUDEnabled || PluginUtils.isBetterHUDEnabled),
    FORMAT_SCOREBOARD("Plugin.formatting.scoreboard", true),

    // WorldEdit
    WORLDEDIT_CUSTOM_BLOCKS("WorldEdit.customblock_mechanic", false),
    WORLDEDIT_FURNITURE("WorldEdit.furniture_mechanic", false),

    // Glyphs
    SHOW_PERMISSION_EMOJIS("Glyphs.emoji_list_permission_only", true),
    UNICODE_COMPLETIONS("Glyphs.unicode_completions", true),
    GLYPH_DEFAULT_PERMISSION("Glyphs.default_permission", "nexo.glyphs.<glyphid>"),
    GLYPH_DEFAULT_FONT("Glyphs.default_permission", Font.MINECRAFT_DEFAULT.asString()),
    GLYPH_HOVER_TEXT("Glyphs.chat_hover_text", "<glyph_placeholder>"),
    SHIFT_FONT("Glyphs.shift_font", "nexo:shift"),

    // Config Tools
    GENERATE_DEFAULT_CONFIGS("ConfigTools.generate_default_configs", true),
    DISABLE_AUTOMATIC_MODEL_DATA("ConfigTools.disable_automatic_model_data", false),
    DISABLE_AUTOMATIC_GLYPH_CODE("ConfigTools.disable_automatic_glyph_code", false),
    INITIAL_CUSTOM_MODEL_DATA("ConfigTools.initial_custom_model_data", 1000),
    SKIPPED_MODEL_DATA_NUMBERS("ConfigTools.skipped_model_data_numbers", emptyList<Int>()),
    ERROR_ITEM("ConfigTools.error_item", mapOf("material" to Material.PODZOL.name, "excludeFromInventory" to false, "injectId" to false)),
    REMOVE_INVALID_FURNITURE("ConfigTools.remove_invalid_furniture", false),

    // Custom Armor
    CUSTOM_ARMOR_TYPE("CustomArmor.type", if (VersionUtil.atleast("1.21.2")) CustomArmorType.COMPONENT else CustomArmorType.TRIMS),
    CUSTOM_ARMOR_ASSIGN("CustomArmor.auto_assign_settings", true),

    // ItemUpdater
    UPDATE_ITEMS("ItemUpdater.update_items", true),
    UPDATE_ITEMS_ON_RELOAD("ItemUpdater.update_items_on_reload", true),
    UPDATE_TILE_ENTITY_CONTENTS("ItemUpdater.update_tile_entity_contents", true),
    UPDATE_ENTITY_CONTENTS("ItemUpdater.update_entity_contents", true),
    OVERRIDE_ITEM_LORE("ItemUpdater.override_item_lore", false),

    //Misc
    RESET_RECIPES("Misc.reset_recipes", true),
    ADD_RECIPES_TO_BOOK("Misc.add_recipes_to_book", true),
    HIDE_SCOREBOARD_NUMBERS("Misc.hide_scoreboard_numbers", false),
    HIDE_SCOREBOARD_BACKGROUND("Misc.hide_scoreboard_background", false),
    HIDE_TABLIST_BACKGROUND("Misc.hide_tablist_background", false),
    BLOCK_OTHER_RESOURCEPACKS("Misc.block_other_resourcepacks", false),

    //Pack
    PACK_GENERATE_ZIP("Pack.generation.generate_zip", true),
    PACK_MINIMIZE_JSON("Pack.generation.minimize_json", true),
    PACK_READER_LENIENT("Pack.generation.read_lenient", true),
    PACK_OBFUSCATION_TYPE("Pack.obfuscation.type", PackObfuscator.Type.SIMPLE),
    PACK_CACHE_OBFUSCATION("Pack.obfuscation.cache", true),
    PACK_IMPORT_DEFAULT("Pack.import.default_assets", true),
    PACK_IMPORT_EXTERNAL("Pack.import.external_packs", true),
    PACK_IMPORT_FROM_LOCATION("Pack.import.from_location", listOf<String>()),
    PACK_IMPORT_FROM_URL("Pack.import.from_url", listOf<String>()),
    PACK_IMPORT_EXTERNAL_PACK_ORDER("Pack.import.external_pack_order", listOf<String>()),
    PACK_IMPORT_MODEL_ENGINE("Pack.import.model_engine.import_pack", true),
    PACK_EXCLUDE_MODEL_ENGINE_SHADERS("Pack.import.model_engine.exclude_shaders", true),
    PACK_EXCLUDED_FILE_EXTENSIONS("Pack.generation.excluded_file_extensions", listOf(".zip", ".tar.gz")),
    PACK_USE_PACKSQUASH("Pack.generation.packsquash.enabled", false),
    PACKSQUASH_EXEC_PATH("Pack.generation.packsquash.executable_path", "plugins/Nexo/pack/packsquash/packsquash"),
    PACKSQUASH_SETTINGS_PATH("Pack.generation.packsquash.settings_path", "plugins/Nexo/pack/packsquash/packsquash.toml"),

    PACK_VALIDATE_MODELS("Pack.validate.models", true),
    PACK_VALIDATE_FONTS("Pack.validate.fonts", true),
    PACK_VALIDATE_ATLAS("Pack.validate.atlas", true),

    PACK_SERVER_TYPE("Pack.server.type", "POLYMATH"),
    SELFHOST_PACK_SERVER_PORT("Pack.server.selfhost.port", 8082),
    SELFHOST_PUBLIC_ADDRESS("Pack.server.selfhost.public_address"),
    SELFHOST_DISPATCH_THREADS("Pack.server.selfhost.dispatch_threads", 10),
    POLYMATH_SERVER("Pack.server.polymath.server", "atlas.nexomc.com"),
    POLYMATH_SECRET("Pack.server.polymath.secret", "nexomc"),
    LOBFILE_API_KEY("Pack.server.lobfile.api_key", "API-KEY"),
    S3_PUBLIC_URL("Pack.server.s3.public_url", "https://public_url.com"),
    S3_REGION("Pack.server.s3.region", "EU_WEST_1"),
    S3_BUCKET_NAME("Pack.server.s3.bucket", "packs"),
    S3_KEY("Pack.server.s3.key", "resource_pack"),
    S3_SECRET_KEY("Pack.server.s3.secret_key", "SECRET"),
    S3_ACCESS_KEY("Pack.server.s3.access_key", "SECRET"),

    PACK_SEND_PRE_JOIN("Pack.dispatch.send_pre_join", VersionUtil.atleast("1.21")),
    PACK_SEND_ON_JOIN("Pack.dispatch.send_on_join", VersionUtil.below("1.21")),
    PACK_SEND_RELOAD("Pack.dispatch.send_on_reload", true),
    PACK_SEND_DELAY("Pack.dispatch.delay", -1),
    PACK_SEND_MANDATORY("Pack.dispatch.mandatory", true),
    PACK_SEND_PROMPT("Pack.dispatch.prompt", "<#fa4943>Accept the pack to enjoy a full <b><gradient:#9055FF:#13E2DA>Nexo</b><#fa4943> experience"),


    // Inventory
    NEXO_INV_LAYOUT("NexoInventory.menu_layout", mapOf(
        "nexo_armor" to mapOf(
            "slot" to 1,
            "icon" to "forest_helmet",
            "displayname" to "<green>Nexo Armor</green>"
        ),
        "nexo_furniture" to mapOf(
            "slot" to 2,
            "icon" to "arm_chair",
            "displayname" to "<green>Nexo Furniture</green>"
        ),
        "nexo_tools" to mapOf(
            "slot" to 3,
            "icon" to "forest_axe",
            "displayname" to "<green>Nexo Tools</green>"
        )
    )),
    NEXO_INV_ROWS("NexoInventory.menu_rows", 6),
    NEXO_INV_SIZE("NexoInventory.menu_size", 45),
    NEXO_INV_TITLE("NexoInventory.main_menu_title", "<shift:-37><glyph:menu_items>"),
    NEXO_INV_NEXT_ICON("NexoInventory.next_page_icon", "next_page_icon"),
    NEXO_INV_PREVIOUS_ICON("NexoInventory.previous_page_icon", "previous_page_icon"),
    NEXO_INV_EXIT("NexoInventory.exit_icon", "cancel_icon"),
    NEXO_RECIPE_SHOWCASE_TITLE("NexoInventory.recipe_showcase_title", "<shift:-7><glyph:menu_recipe>")
    ;

    val path: String
    private val defaultValue: Any?
    private var comments = listOf<String>()
    private var inlineComments = listOf<String>()
    private var richComment: Component = Component.empty()

    constructor(path: String) {
        this.path = path
        this.defaultValue = null
    }

    constructor(path: String, defaultValue: Any?) {
        this.path = path
        this.defaultValue = defaultValue
    }

    constructor(path: String, defaultValue: Any?, vararg comments: String) {
        this.path = path
        this.defaultValue = defaultValue
        this.comments = listOf(*comments)
    }

    constructor(path: String, defaultValue: Any?, comments: List<String>, vararg inlineComments: String) {
        this.path = path
        this.defaultValue = defaultValue
        this.comments = comments
        this.inlineComments = listOf(*inlineComments)
    }

    constructor(path: String, defaultValue: Any?, richComment: Component) {
        this.path = path
        this.defaultValue = defaultValue
        this.richComment = richComment
    }

    private var _value: Any? = null
    var value: Any?
        get() {
            if (_value == null) _value = NexoPlugin.instance().configsManager().settings().get(path)
            return _value
        }
        set(newValue) {
            setValue(newValue, true)
        }

    fun setValue(value: Any?, save: Boolean) {
        val settingFile = NexoPlugin.instance().configsManager().settings()
        settingFile.set(path, value)
        runCatching {
            if (save) settingFile.save(NexoPlugin.instance().dataFolder.resolve("settings.yml"))
        }.onFailure {
            Logs.logError("Failed to apply changes to settings.yml")
        }
    }

    override fun toString() = value.toString()

    fun toString(optionalDefault: String) = value as? String ?: optionalDefault

    fun toKey() = runCatching { Key.key(toString()) }.getOrDefault(Key.key("minecraft:default"))
    fun toKey(optionalDefault: Key) = runCatching { Key.key(toString()) }.getOrDefault(optionalDefault)

    fun <E : Enum<E>> toEnum(enumClass: Class<E>, defaultValue: E): E =
        EnumUtils.getEnum(enumClass, toString().uppercase(), defaultValue)

    fun <E : Enum<E>> toEnumOrGet(
        enumClass: Class<E>,
        fallback: (String) -> E
    ): E = toString().toEnumOrElse(enumClass, fallback)

    fun toComponent() = AdventureUtils.MINI_MESSAGE.deserialize(value.toString())

    fun toInt() = toInt(-1)

    /**
     * @param optionalDefault value to return if the path is not an integer
     * @return the value of the path as an int, or the default value if the path is not found
     */
    fun toInt(optionalDefault: Int) = runCatching {
        value.toString().toIntOrNull()
    }.getOrNull() ?: optionalDefault

    fun toBool(defaultValue: Boolean) = value as? Boolean ?: defaultValue

    fun toBool() = value as? Boolean ?: false

    fun toStringList() = NexoPlugin.instance().configsManager().settings().getStringList(path)

    fun toConfigSection() = NexoPlugin.instance().configsManager().settings().getConfigurationSection(path)

    companion object {
        fun reload() {
            Settings.entries.forEach { it._value = null }
        }
        fun validateSettings(): YamlConfiguration {
            val settingsFile = NexoPlugin.instance().dataFolder.toPath().resolve("settings.yml").toFile()
            val settings = if (settingsFile.exists()) loadConfiguration(settingsFile) else YamlConfiguration()

            settings.options().copyDefaults(true).indent(2).parseComments(true)
            settings.addDefaults(defaultSettings())

            runCatching {
                settingsFile.createNewFile()
                settings.save(settingsFile)
            }.onFailure {
                if (settings.getBoolean("debug")) it.printStackTrace()
            }

            return settings
        }

        private fun defaultSettings(): YamlConfiguration {
            return YamlConfiguration().apply {
                options().copyDefaults(true).indent(4).parseComments(true)

                entries.forEach { setting ->
                    set(setting.path, (setting.defaultValue as? Enum<*>)?.name ?: setting.defaultValue)
                    setComments(setting.path, setting.comments)
                    setInlineComments(setting.path, setting.inlineComments)
                }
            }
        }
    }
}
