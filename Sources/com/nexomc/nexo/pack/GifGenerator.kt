package com.nexomc.nexo.pack

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.glyphs.AnimatedGlyph
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.base.Writable
import team.unnamed.creative.metadata.overlays.OverlayEntry
import team.unnamed.creative.metadata.overlays.OverlaysMeta
import team.unnamed.creative.metadata.pack.PackFormat
import team.unnamed.creative.overlay.Overlay

class GifGenerator(private val resourcePack: ResourcePack) {
    private val classLoader = NexoPlugin.instance().javaClass.classLoader

    fun generateGifFiles() {
        NexoPlugin.instance().fontManager().glyphs().filterIsInstance<AnimatedGlyph>().forEach { glyph ->
            glyph.generateSplitGif(resourcePack)
        }

        if (Settings.GENERATE_GIF_SHADERS.toBool()) generateShaderFiles()
    }

    private fun generateShaderFiles() {
        val overlays = resourcePack.overlaysMeta()?.entries() ?: mutableListOf()
        overlays += OverlayEntry.of(PackFormat.format(34, 32, 34), "nexo_1_21_1")
        overlays += OverlayEntry.of(PackFormat.format(42, 42, 46), "nexo_1_21_3")
        resourcePack.overlaysMeta(OverlaysMeta.of(overlays))

        resourcePack.overlay(Overlay.overlay("nexo_1_21_1").apply { writables("v1_21_1") })
        resourcePack.overlay(Overlay.overlay("nexo_1_21_3").apply { writables("v1_21_3") })
    }

    private val path = "assets/minecraft/shaders/core/rendertype_text"
    private fun Overlay.writables(version: String) {
        unknownFile("$path.json", Writable.resource(classLoader, "gifs/$version/rendertype_text.json"))
        unknownFile("$path.vsh", Writable.resource(classLoader, "gifs/$version/rendertype_text.vsh"))
        unknownFile("$path.fsh", Writable.resource(classLoader, "gifs/$version/rendertype_text.fsh"))
    }
}