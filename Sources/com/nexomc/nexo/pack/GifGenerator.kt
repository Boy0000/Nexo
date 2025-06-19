package com.nexomc.nexo.pack

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.glyphs.AnimatedGlyph
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.base.Writable
import team.unnamed.creative.overlay.Overlay

class GifGenerator(private val resourcePack: ResourcePack) {
    private val classLoader = NexoPlugin.instance().javaClass.classLoader

    fun generateGifFiles() {
        NexoPlugin.instance().fontManager().glyphs().filterIsInstance<AnimatedGlyph>().forEach { glyph ->
            glyph.generateSplitGif(resourcePack)
        }

        if (Settings.GENERATE_GIF_SHADERS.toBool()) {
            NexoOverlay.V1_21_1.overlay.writables("v1_21_1")
            NexoOverlay.V1_21_3.overlay.writables("v1_21_3")
            NexoOverlay.V1_21_6.overlay.writables("v1_21_6")
        }
    }

    private fun Overlay.writables(version: String) {
        var path = "assets/minecraft/shaders/core/rendertype_text"
        unknownFile("$path.json", Writable.resource(classLoader, "gifs/$version/rendertype_text.json"))
        unknownFile("$path.vsh", Writable.resource(classLoader, "gifs/$version/rendertype_text.vsh"))
        unknownFile("$path.fsh", Writable.resource(classLoader, "gifs/$version/rendertype_text.fsh"))

        path += "_see_through"
        unknownFile("$path.json", Writable.resource(classLoader, "gifs/$version/rendertype_text_see_through.json"))
        unknownFile("$path.vsh", Writable.resource(classLoader, "gifs/$version/rendertype_text_see_through.vsh"))
        unknownFile("$path.fsh", Writable.resource(classLoader, "gifs/$version/rendertype_text_see_through.fsh"))
    }
}