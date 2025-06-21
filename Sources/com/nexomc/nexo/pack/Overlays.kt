package com.nexomc.nexo.pack

import team.unnamed.creative.ResourcePack
import team.unnamed.creative.metadata.overlays.OverlayEntry
import team.unnamed.creative.metadata.overlays.OverlaysMeta
import team.unnamed.creative.metadata.pack.PackFormat
import team.unnamed.creative.overlay.Overlay

enum class NexoOverlay(val id: String, val format: Int) {
    V1_20_4("nexo_1_20_4", 22),
    V1_20_6("nexo_1_20_6", 32),
    V1_21_1("nexo_1_21_1", 34),
    V1_21_3("nexo_1_21_3", 42),
    V1_21_4("nexo_1_21_4", 46),
    V1_21_5("nexo_1_21_5", 55),
    V1_21_6("nexo_1_21_6", 63);

    val overlay: Overlay = Overlay.overlay(id)
    val entry: OverlayEntry = OverlayEntry.of(PackFormat.format(format, format, 99), id)
}

object Overlays {
    fun addToResourcepack(resourcePack: ResourcePack) {
        val entries = resourcePack.overlaysMeta()?.entries() ?: mutableListOf()
        NexoOverlay.entries.forEach {
            resourcePack.overlay(it.overlay)
            entries += it.entry
        }
        resourcePack.overlaysMeta(OverlaysMeta.of(entries.distinctBy { it.directory() }))
    }
}

