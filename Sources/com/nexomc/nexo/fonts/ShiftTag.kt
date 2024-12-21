package com.nexomc.nexo.fonts

import com.nexomc.nexo.utils.substringBetween
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import java.util.regex.Pattern

object ShiftTag {
    private const val SHIFT = "shift"
    private const val SHIFT_SHORT = "s"
    val FONT = Key.key("nexo:shift")
    val PATTERN = Pattern.compile("(<shift:(-?\\d+)>)")

    val RESOLVER = TagResolver.resolver(setOf(SHIFT, SHIFT_SHORT)) { args, ctx -> shiftTag(args) }

    val REPLACEMENT_CONFIG = TextReplacementConfig.builder()
        .match(PATTERN).replacement { r, _ -> Component.text(Shift.of(r.group(1).substringBetween("<shift:",">").toIntOrNull() ?: 0)).font(FONT) }.build()

    private fun shiftTag(args: ArgumentQueue): Tag {
        val shift = args.popOr("A shift value is required").value().toIntOrNull() ?: 0
        return Tag.selfClosingInserting(Component.text(Shift.of(shift)).font(FONT))
    }
}
