package com.nexomc.nexo.pack.shaders

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.glyphs.AnimatedGlyph
import com.nexomc.nexo.pack.NexoOverlay
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
            NexoOverlay.V1_21_4.overlay.writables("v1_21_3")
            NexoOverlay.V1_21_5.overlay.writables("v1_21_3")
            NexoOverlay.V1_21_6.overlay.writables("v1_21_6")
        }
    }

    private fun Overlay.writables(version: String) {
        val basePath = "assets/minecraft/shaders/core"
        val baseName = "shaders/gifs/$version"

        fun writeShaderFiles(suffix: String) {
            val fullPath = "$basePath/$suffix"
            val fullName = "$baseName/$suffix"
            listOf("json", "vsh", "fsh").forEach { ext ->
                unknownFile("$fullPath.$ext", Writable.resource(classLoader, "$fullName.$ext"))
            }
        }

        writeShaderFiles("rendertype_text")
        writeShaderFiles("rendertype_text_see_through")
    }
}