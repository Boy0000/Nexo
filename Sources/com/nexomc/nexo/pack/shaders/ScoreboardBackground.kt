package com.nexomc.nexo.pack.shaders

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.pack.NexoOverlay
import com.nexomc.nexo.utils.applyIf
import com.nexomc.nexo.utils.remove
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.base.Writable
import team.unnamed.creative.overlay.Overlay

class ScoreboardBackground(private val resourcePack: ResourcePack) {
    private val classLoader = NexoPlugin.instance().javaClass.classLoader

    private val scoreboard = Settings.HIDE_SCOREBOARD_BACKGROUND.toBool()
    private val tablist = Settings.HIDE_TABLIST_BACKGROUND.toBool()

    fun generateFiles() {
        if (scoreboard || tablist) {
            NexoOverlay.V1_21_1.overlay.writables("v1_21_1", "rendertype_gui.vsh")
            NexoOverlay.V1_21_3.overlay.writables("v1_21_1", "rendertype_gui.vsh")
            NexoOverlay.V1_21_4.overlay.writables("v1_21_1", "rendertype_gui.vsh")
            NexoOverlay.V1_21_5.overlay.writables("v1_21_1", "rendertype_gui.vsh")
            NexoOverlay.V1_21_6.overlay.writables("v1_21_6", "gui.vsh")
        }
    }

    private fun Overlay.writables(version: String, fileName: String) {
     val shader = Writable.resource(classLoader, "shaders/score_tab/$version/$fileName").toUTF8String()
            .applyIf(scoreboard) {
                remove("//SCOREBOARD ")
            }.applyIf(tablist) {
                remove("//TABLIST ")
            }

        unknownFile("assets/minecraft/shaders/core/$fileName", Writable.stringUtf8(shader))
    }
}