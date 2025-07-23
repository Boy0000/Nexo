package com.nexomc.nexo.dialog

import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.body.PlainMessageDialogBody
import net.kyori.adventure.text.Component
import org.bukkit.configuration.ConfigurationSection
import org.jetbrains.annotations.Range

data class NexoDialogBody(val bodySection: ConfigurationSection) {

    fun createDialogBody(): DialogBody {
        val type = runCatching { DialogBodyTypes.valueOf(bodySection.getString("type")!!) }.getOrDefault(DialogBodyTypes.MESSAGE)

        return when (type) {
            DialogBodyTypes.MESSAGE -> {
                val message = bodySection.getRichMessage("message") ?: Component.empty()
                DialogBody.plainMessage(message, bodySection.getInt("width", 200))
            }
            DialogBodyTypes.ITEM -> {
                val item = NexoDialogItem(bodySection).buildItem()
                val description = bodySection.getString("description")
                val messageBody = object : PlainMessageDialogBody {
                    override fun contents(): Component = bodySection.getRichMessage("description.contents") ?: Component.empty()
                    override fun width(): @Range(from = 1, to = 1024) Int = bodySection.getInt("width", 200)
                }.takeIf { description != null }
                val showDecorations = bodySection.getBoolean("showDecorations", true)
                val showTooltip = bodySection.getBoolean("showTooltip", true)
                val width = bodySection.getInt("width", 16)
                val height = bodySection.getInt("height", 16)

                DialogBody.item(item, messageBody, showDecorations, showTooltip, width, height)
            }
        }
    }
}