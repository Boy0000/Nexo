package com.nexomc.nexo.configs

enum class RemovedSettings(private val path: String) {
    ATTEMPT_TO_MIGRATE_DUPLICATES("Pack.generation.attempt_to_migrate_duplicates"),
    AUTOMATICALLY_SET_MODEL_DATA("ConfigsTools.automatically_set_model_data"),
    AUTOMATICALLY_SET_GLYPH_CODE("ConfigsTools.automatically_set_glyph_code"),
    MERGE_FONTS("Pack.import.merge_font_files"),
    AUTO_UPDATE_ITEMS("ItemUpdater.auto_update_items"),
    OVERRIDE_LORE("ItemUpdater.override_lore"),

    UPDATE_FURNITURE("FurnitureUpdater.update_furniture"),
    UPDATE_FURNITURE_ON_RELOAD("ItemUpdater.update_furniture_on_reload"),
    UPDATE_FURNITURE_ON_LOAD("ItemUpdater.update_furniture_on_load"),
    FURNITURE_UPDATE_DELAY("ItemUpdater.furniture_update_delay_in_seconds"),
    FURNITURE_UPDATE_DELAY2("FurnitureUpdater.furniture_update_delay_in_seconds"),
    UPDATE_FURNITURE_ON_LOAD2("FurnitureUpdater.update_furniture_on_load"),
    UPDATE_FURNITURE_ON_RELOAD2("FurnitureUpdater.update_furniture_on_reload"),

    SEND_PACK_ADVANCED("Pack.dispatch.send_pack_advanced"),
    SHIELD_DISPLAY("Misc.shield_display"),
    BOW_DISPLAY("Misc.bow_display"),
    CROSSBOW_DISPLAY("Misc.crossbow_display"),
    GENERATE_ATLAS_FILE("Pack.generation.atlas.generate"),
    EXCLUDE_MALFORMED_ATLAS("Pack.generation.atlas.exclude_malformed_from_atlas"),
    ATLAS_GENERATION_TYPE("Pack.generation.atlas.type"),
    ARMOR_EQUIP_EVENT_BYPASS("Misc.armor_equip_event_bypass"),
    UPLOAD_TYPE("Pack.upload.type"),
    UPLOAD("Pack.upload.enabled"),
    UPLOAD_OPTIONS("Pack.upload.options"),
    POLYMATH_SERVER("Pack.upload.polymath.server"),

    EXPERIMENTAL_FIX_BROKEN_FURNITURE("FurnitureUpdater.experimental_fix_broken_furniture"),
    EXPERIMENTAL_FURNITURE_TYPE_UPDATE("FurnitureUpdater.experimental_furniture_type_update"),

    SEND_JOIN_MESSAGE("Pack.dispatch.join_message.enabled"),
    JOIN_MESSAGE_DELAY("Pack.dispatch.join_message.delay"),
    GENERATE_DEFAULT_ASSETS("Plugin.generation.default_assets"),

    VERIFY_PACK_FILES("Pack.generation.verify_pack_files"),
    GENERATE_MODEL_BASED_ON_TEXTURE_PATH("Pack.generation.auto_generated_models_follow_texture_path"),
    COMPRESSION("Pack.generation.compression"),
    PROTECTION("Pack.generation.protection"),

    RECEIVE_ENABLED("Pack.receive.enabled"),
    RECEIVE_ALLOWED_ACTIONS("Pack.receive.accepted.actions"),
    RECEIVE_LOADED_ACTIONS("Pack.receive.loaded.actions"),
    RECEIVE_FAILED_ACTIONS("Pack.receive.failed_download.actions"),
    RECEIVE_DENIED_ACTIONS("Pack.receive.denied.actions"),
    RECEIVE_FAILED_RELOAD_ACTIONS("Pack.receive.failed_reload.actions"),
    RECEIVE_DOWNLOADED_ACTIONS("Pack.receive.downloaded.actions"),
    RECEIVE_INVALID_URL_ACTIONS("Pack.receive.invalid_url.actions"),
    RECEIVE_DISCARDED_ACTIONS("Pack.receive.discarded.actions"),

    BLOCK_CORRECTION("CustomBlocks.block_correction"),
    DOWNLOAD_DEFAULT_ASSETS("Plugin.default_content.download_resourcepack"),

    CUSTOM_ARMOR_TRIMS_SETTINGS("CustomArmor.trims_settings"),
    CUSTOM_ARMOR_TRIMS_ASSIGN("CustomArmor.trims_settings.auto_assign_settings"),
    DISABLE_LEATHER_REPAIR_CUSTOM("CustomArmor.disable_leather_repair"),
    CUSTOM_ARMOR_SHADER_SETTINGS("CustomArmor.shader_settings"),
    CUSTOM_ARMOR_COMPONENT_SETTINGS("CustomArmor.component_settings"),

    FORMAT_INVENTORY_TITLES("Plugin.formatting.inventory_titles"),
    FORMAT_TITLES("Plugin.formatting.titles"),
    FORMAT_SUBTITLES("Plugin.formatting.subtitles"),
    FORMAT_ACTION_BAR("Plugin.formatting.action_bar"),
    PACK_GENERATE("Pack.generation.generate"),
    UPDATE_CONFIGS("ConfigsTool.enable_configs_updater"),
    GLYPH_HANDLER("Glyphs.glyph_handler"),
    TRIMS_ARMOR("CustomArmor.trims_settings"),
    CUSTOM_BLOCKS("CustomBlocks"),
    PACK_RECEIVE("Pack.receive"),
    FORCE_UNICODE("Pack.generation.fix_force_unicode_glyphs"),
    PACK_COMMENT("Pack.generation.comment"),
    PACK_SLICER("Pack.generation.texture_slicer"),
    PACK_UPLOAD("Pack.upload"),
    OVERRIDE_RENAMED_ITEMS("ItemUpdater.override_renamed_items"),
    PACK_IMPORT_MODEL_ENGINE("Pack.import.modelengine"),
    PACK_READER_LENIENT("Pack.generation.lenient"),
    WORLDEDIT_NOTEBLOCKS("WorldEdit.noteblock_mechanic"),
    WORLDEDIT_STRINGBLOCKS("WorldEdit.stringblock_mechanic"),

    // Chat
    CHAT_HANDLER("Chat.chat_handler"),
    FORMAT_PACKETS("Plugin.formatting.packets"),
    GLYPH_HOVER_TEXT("Glyphs.chat_hover_text"),
    ;

    override fun toString(): String {
        return this.path
    }

    companion object {
        fun toStringList() = entries.map { it.toString() }
    }
}
