package com.nexomc.nexo.glyphs

data class ReferenceGlyph(
    val glyph: Glyph,
    val referenceId: String,
    val index: IntRange,
    val _permission: String,
    val _placeholders: List<String>
) : Glyph(
    referenceId,
    glyph.font,
    glyph.texture,
    glyph.ascent,
    glyph.height,
    listOf(glyph.unicodes.joinToString("").substring(index.first - 1, index.last)),
    _permission,
    _placeholders
)