package com.nexomc.nexo.dialog

import com.nexomc.nexo.utils.*
import com.nexomc.nexo.utils.EnumUtils.toEnumOrElse
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.type.DialogType
import io.papermc.paper.registry.event.RegistryEvents
import io.papermc.paper.registry.set.RegistrySet
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration

object NexoDialogs {
    @JvmStatic
    fun registerDialogs(context: BootstrapContext) {
        runCatching {
            NexoDialogItem.registerItemConfigs(context)
            val dialogDirectory = context.dataDirectory.resolve("dialogs").toFile().apply { mkdirs() }
            val dialogFiles = dialogDirectory.listYamlFiles(true)
            if (dialogFiles.isEmpty()) return

            context.lifecycleManager.registerEventHandler(RegistryEvents.DIALOG.compose().newHandler { handler ->
                dialogFiles.forEach { file ->
                    val dialogKey = Key.key("nexo", file.nameWithoutExtension.lowercase())
                    val typedKey = TypedKey.create(RegistryKey.DIALOG, dialogKey)
                    val dialogConfig = runCatching { YamlConfiguration.loadConfiguration(file) }.getOrNull() ?: return@forEach
                    val dialogBase = dialogConfig.getConfigurationSection("base")?.let(::dialogBase) ?: return@forEach

                    handler.registry().register(typedKey) { builder ->
                        builder.type(dialogType(dialogConfig)).base(dialogBase)
                    }
                }
            })
        }.onFailure {
            context.logger.error("Error while loading Custom Nexo Dialogs", it)
        }
    }

    private fun dialogType(dialogConfig: ConfigurationSection): DialogType {
        val type = dialogConfig.getStringOrNull("type")?.toEnumOrElse(DialogTypes::class.java) { DialogTypes.NOTICE }
            ?: return DialogType.notice()

        return when (type) {
            DialogTypes.NOTICE -> {
                val action = NexoActionButton.createButton(dialogConfig.getConfigurationSection("action"))

                DialogType.notice(action)
            }
            DialogTypes.CONFIRM -> {
                val yesButton = NexoActionButton.createButton(dialogConfig.getConfigurationSection("yesButton"))
                val noButton = NexoActionButton.createButton(dialogConfig.getConfigurationSection("noButton"))

                DialogType.confirmation(yesButton, noButton)
            }
            DialogTypes.LINK -> {
                val exitAction = NexoActionButton.createButton(dialogConfig.getConfigurationSection("exitAction"))
                val columns = dialogConfig.getInt("columns").coerceAtLeast(1)
                val buttonWidth = dialogConfig.getInt("buttonWidth", 200).coerceIn(1, 1024)

                DialogType.serverLinks(exitAction, columns, buttonWidth)
            }
            DialogTypes.MULTI -> {
                val actions = dialogConfig.sectionList("actions").map(NexoActionButton::createButton)
                val exitAction = NexoActionButton.createButton(dialogConfig.getConfigurationSection("exitAction"))
                val columns = dialogConfig.getInt("columns").coerceAtLeast(1)

                DialogType.multiAction(actions, exitAction, columns)
            }
            DialogTypes.LIST -> {
                val dialogRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.DIALOG)
                val dialogs = dialogConfig.getKeyList("dialogs").mapNotNull(dialogRegistry::get)
                val registrySet = RegistrySet.valueSet(RegistryKey.DIALOG, dialogs)
                val exitAction = NexoActionButton.createButton(dialogConfig.getConfigurationSection("exitAction"))
                val columns = dialogConfig.getInt("columns").coerceAtLeast(1)
                val buttonWidth =dialogConfig.getInt("buttonWidth", 200).coerceIn(1, 1024)

                DialogType.dialogList(registrySet, exitAction, columns, buttonWidth)
            }
        }
    }

    private fun dialogBase(baseConfig: ConfigurationSection): DialogBase {
        val title = baseConfig.getRichMessage("title") ?: Component.empty()
        val externalTitle = baseConfig.getRichMessage("externalTitle")
        val canCloseWithEscape = baseConfig.getBoolean("canCloseWithEscape")
        val pause = baseConfig.getBoolean("pause")
        val afterAction = baseConfig.getEnum("afterAction", DialogBase.DialogAfterAction::class.java) ?: DialogBase.DialogAfterAction.CLOSE
        val body = baseConfig.sectionList("bodies").map { NexoDialogBody(it).createDialogBody() }
        val inputs = baseConfig.sectionList("inputs").mapNotNull { NexoDialogInput(it).createDialogInput() }

        return DialogBase.create(title, externalTitle, canCloseWithEscape, pause, afterAction, body, inputs)
    }
}