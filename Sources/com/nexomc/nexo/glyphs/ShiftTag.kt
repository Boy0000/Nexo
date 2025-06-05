package com.nexomc.nexo.glyphs

import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.substringBetween
import java.util.regex.Pattern
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

object ShiftTag {
    private const val SHIFT = "shift"
    private const val SHIFT_SHORT = "s"
    val FONT = Settings.SHIFT_FONT.toKey(Key.key("nexo:shift"))
    val PATTERN: Pattern = Pattern.compile("(<shift:(-?\\d+)>)")
    val ESCAPED_PATTERN: Pattern = Pattern.compile("(\\\\<shift:(-?\\d+)>)")

    val RESOLVER = TagResolver.resolver(setOf(SHIFT, SHIFT_SHORT)) { args, ctx -> shiftTag(args) }

    val REPLACEMENT_CONFIG = TextReplacementConfig.builder()
        .match(PATTERN).replacement { r, _ -> Component.text(Shift.of(r.group(1).substringBetween("<shift:",">").toIntOrNull() ?: 0)) }.build()

    val ESCAPE_REPLACEMENT_CONFIG = TextReplacementConfig.builder()
        .match(ESCAPED_PATTERN).replacement { r, b -> b.content(r.group(1).removePrefix("\\\\")) }.build()

    private fun shiftTag(args: ArgumentQueue): Tag {
        val shift = args.popOr("A shift value is required").value().toIntOrNull() ?: 0
        return Tag.selfClosingInserting(Component.text(Shift.of(shift)).font(FONT))
    }
}
