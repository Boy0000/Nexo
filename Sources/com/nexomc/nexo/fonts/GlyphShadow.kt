package com.nexomc.nexo.fonts

import com.nexomc.nexo.commands.toColor
import com.nexomc.nexo.configs.Settings
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor
import org.bukkit.Color

class GlyphShadow(val color: Color? = Settings.GLYPH_DEFAULT_SHADOW_COLOR.toString().toColor()) {

    fun applyTo(component: Component): Component {
        return runCatching {
            component.shadowColor(color?.asARGB()?.let(ShadowColor::shadowColor))
        }.getOrDefault(component)
    }

    companion object {
        val DEFAULT = GlyphShadow()
        fun Component.glyphShadow(shadow: GlyphShadow?): Component = (shadow ?: DEFAULT).applyTo(this)
    }
}