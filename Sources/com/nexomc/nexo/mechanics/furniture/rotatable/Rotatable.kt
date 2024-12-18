package com.nexomc.nexo.mechanics.furniture.rotatable

import com.nexomc.nexo.utils.safeCast
import org.bukkit.configuration.ConfigurationSection

data class Rotatable(val rotatable: Boolean = false, val onSneak: Boolean = false) {
    constructor(rotatable: Any) : this(
        rotatable.safeCast<Boolean>()
            ?: rotatable.safeCast<ConfigurationSection>()?.getBoolean("rotatable")
            ?: rotatable.safeCast<Map<String, Boolean>>()?.get("rotatable")
            ?: false,
        rotatable.safeCast<ConfigurationSection>()?.getBoolean("on_sneak")
            ?: rotatable.safeCast<Map<String, Boolean>>()?.get("on_sneak")
            ?: false
    )
}