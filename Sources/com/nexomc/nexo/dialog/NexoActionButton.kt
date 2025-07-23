package com.nexomc.nexo.dialog

import io.papermc.paper.registry.data.dialog.ActionButton
import net.kyori.adventure.text.Component
import org.bukkit.configuration.ConfigurationSection

object NexoActionButton {

    fun createButton(dialogConfig: ConfigurationSection?): ActionButton {
        return ActionButton.builder(dialogConfig?.getRichMessage("label") ?: Component.empty())
            .tooltip(dialogConfig?.getRichMessage("tooltip"))
            .width(dialogConfig?.getInt("width")?.coerceIn(1, 1024) ?: 200)
            .action(null)
            .build()
    }

}