package com.nexomc.nexo.dialog

import com.nexomc.nexo.utils.getEnum
import com.nexomc.nexo.utils.getKey
import com.nexomc.nexo.utils.getStringOrNull
import io.papermc.paper.registry.data.dialog.action.DialogAction
import net.kyori.adventure.nbt.api.BinaryTagHolder
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.configuration.ConfigurationSection

class NexoDialogAction(val actionSection: ConfigurationSection) {

    fun createAction(): DialogAction? {
        val type = actionSection.getEnum("type", DialogActionTypes::class.java) ?: run {
            NexoDialogs.context.logger.warn("Invalid ActionType in ${actionSection.currentPath}, ignoring action...")
            return null
        }
        return when (type) {
            DialogActionTypes.OPEN_URL -> DialogAction.staticAction(ClickEvent.openUrl(actionSection.getString("url", "")!!))
            DialogActionTypes.RUN_COMMAND -> DialogAction.staticAction(ClickEvent.runCommand(actionSection.getString("command", "")!!))
            DialogActionTypes.SUGGEST_COMMAND -> DialogAction.staticAction(ClickEvent.suggestCommand(actionSection.getString("command")!!))
            DialogActionTypes.CHANGE_PAGE -> DialogAction.staticAction(ClickEvent.changePage(actionSection.getInt("page")))
            DialogActionTypes.CLIPBOARD -> DialogAction.staticAction(ClickEvent.copyToClipboard(actionSection.getString("clipboard")!!))
            DialogActionTypes.SHOW_DIALOG -> {
                val dialog = actionSection.getKey("dialog")?.let(NexoDialogs.dialogRegistry::get) ?: run {
                    NexoDialogs.context.logger.warn("Invalid Dialog specified in ${actionSection.currentPath}, ignoring action...")
                    return null
                }
                DialogAction.staticAction(ClickEvent.showDialog(dialog))
            }
            DialogActionTypes.CUSTOM -> {
                val id = actionSection.getKey("id") ?: run {
                    NexoDialogs.context.logger.warn("Invalid id in ${actionSection.currentPath}, ignoring action...")
                    return null
                }
                val payload = actionSection.getStringOrNull("payload")?.let(BinaryTagHolder::binaryTagHolder) ?: run {
                    NexoDialogs.context.logger.warn("Invalid payload in ${actionSection.currentPath}, ignoring action...")
                    return null
                }
                DialogAction.staticAction(ClickEvent.custom(id, payload))
            }

            DialogActionTypes.DYNAMIC_RUN_COMMAND -> DialogAction.commandTemplate(actionSection.getString("template", "")!!)
            DialogActionTypes.DYNAMIC_CUSTOM -> {
                val id = actionSection.getKey("id") ?: run {
                    NexoDialogs.context.logger.warn("Invalid id in ${actionSection.currentPath}, ignoring action...")
                    return null
                }
                val additions = actionSection.getString("additions")?.let(BinaryTagHolder::binaryTagHolder)
                DialogAction.customClick(id, additions)
            }
            else -> null
        }
    }
}