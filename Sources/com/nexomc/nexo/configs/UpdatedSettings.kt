package com.nexomc.nexo.configs

import org.apache.commons.lang.StringUtils

enum class UpdatedSettings(private val path: String, private val newPath: String) {
    PACK_SEND_ON_JOIN("Pack.dispatch.send_pack", "Pack.dispatch.send_on_join"),
    GENERATE_DEFAULT_CONFIGS("Plugin.default_content.default_configs", "ConfigTools.generate_default_configs"),
    NEXO_INV("nexo_inventory", "NexoInventory"),
    ;

    override fun toString() = this.path + ", " + newPath

    companion object {
        fun toStringMap() = entries.associate { u -> u.toString().split(", ", limit = 2).let { it[0] to it[1] } }
    }
}
