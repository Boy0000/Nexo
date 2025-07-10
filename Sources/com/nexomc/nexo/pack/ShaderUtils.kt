package com.nexomc.nexo.pack

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.utils.applyIf
import com.nexomc.nexo.utils.remove
import team.unnamed.creative.base.Writable

class ShaderUtils {
    object ScoreboardBackground {

        private val shaderFile by lazy {
            NexoPlugin.instance().getResource("shaders/score_tab/gui.vsh")?.readAllBytes()?.decodeToString() ?: ""
        }

        fun addOverlay(nexoOverlay: NexoOverlay, filePrefix: String) {
            if (shaderFile.isEmpty()) return
            val shaderContent = shaderFile.applyIf(Settings.HIDE_TABLIST_BACKGROUND.toBool()) {
                remove("//TABLIST")
            }.applyIf(Settings.HIDE_SCOREBOARD_BACKGROUND.toBool()) {
                remove("//SCOREBOARD")
            }
            val writable = Writable.stringUtf8(shaderContent)
            nexoOverlay.overlay.unknownFile("assets/minecraft/shaders/core/$filePrefix.vsh", writable)
        }
    }
}
