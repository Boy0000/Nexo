package com.nexomc.nexo.commands

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.configs.SoundManager
import com.nexomc.nexo.fonts.FontManager
import com.nexomc.nexo.items.ItemUpdater
import com.nexomc.nexo.mechanics.MechanicsManager
import com.nexomc.nexo.mechanics.custom_block.CustomBlockSoundListener
import com.nexomc.nexo.mechanics.furniture.FurnitureFactory
import com.nexomc.nexo.mechanics.furniture.listeners.FurnitureSoundListener
import com.nexomc.nexo.nms.NMSHandlers
import com.nexomc.nexo.pack.PackGenerator
import com.nexomc.nexo.pack.server.NexoPackServer
import com.nexomc.nexo.recipes.RecipesManager
import com.nexomc.nexo.utils.AdventureUtils.tagResolver
import com.nexomc.nexo.utils.SchedulerUtils
import com.nexomc.nexo.utils.logs.Logs
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.multiLiteralArgument
import dev.jorel.commandapi.kotlindsl.textArgument
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.command.CommandSender
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import kotlin.jvm.optionals.getOrDefault

internal fun CommandTree.reloadCommand() = multiLiteralArgument(nodeName = "reload", "reload", "rl") {
    withPermission("nexo.command.reload")
    textArgument("type", optional = true) {
        replaceSuggestions(ArgumentSuggestions.strings("items", "pack", "recipes", "configs", "all"))
        anyExecutor { sender, args ->
            when ((args.getOptional("type").getOrDefault("all") as String).lowercase()) {
                "items" -> ReloadCommand.reloadItems(sender)
                "pack" -> ReloadCommand.reloadPack(sender)
                "recipes" -> ReloadCommand.reloadRecipes(sender)
                "configs" -> ReloadCommand.reloadConfigs(sender)
                else -> ReloadCommand.reloadAll(sender)
            }
            Bukkit.getOnlinePlayers().forEach { player: Player ->
                NexoPlugin.instance().fontManager().sendGlyphTabCompletion(player)
            }
        }
    }
}

object ReloadCommand {

    @JvmOverloads
    @JvmStatic
    fun reloadAll(sender: CommandSender? = Bukkit.getConsoleSender()) {
        FurnitureFactory.instance()?.packetManager()?.removeAllFurniturePackets()
        reloadConfigs(sender)
        reloadItems(sender)
        reloadRecipes(sender)
        reloadPack(sender)
    }

    @JvmOverloads
    @JvmStatic
    fun reloadItems(sender: CommandSender? = Bukkit.getConsoleSender()) {
        NexoItems.itemConfigCache.clear()
        CustomBlockSoundListener.breakerPlaySound.onEach { it.value.cancel() }.clear()
        FurnitureSoundListener.breakerPlaySound.onEach { it.value.cancel() }.clear()
        FurnitureFactory.instance()?.packetManager()?.removeAllFurniturePackets()
        NexoItems.loadItems()
        NexoPlugin.instance().invManager().regen()

        if (Settings.UPDATE_ITEMS.toBool() && Settings.UPDATE_ITEMS_ON_RELOAD.toBool()) {
            SchedulerUtils.runAtWorldEntities(ItemUpdater::updateEntityInventories)
        }

        SchedulerUtils.runAtWorldEntities { entity ->
            (entity as? ItemDisplay)?.let(NexoFurniture::updateFurniture)
        }

        Message.RELOAD.send(sender, tagResolver("reloaded", "items"))
    }

    @JvmOverloads
    @JvmStatic
    fun reloadPack(sender: CommandSender? = Bukkit.getConsoleSender()) {
        Message.PACK_REGENERATED.send(sender)
        NexoPlugin.instance().fontManager(FontManager(NexoPlugin.instance().configsManager()))
        NexoPlugin.instance().soundManager(SoundManager(NexoPlugin.instance().configsManager().sounds))
        NexoPlugin.instance().packGenerator(PackGenerator())
        NexoPlugin.instance().packGenerator().generatePack()
    }

    @JvmOverloads
    @JvmStatic
    fun reloadRecipes(sender: CommandSender? = Bukkit.getConsoleSender()) {
        if (Bukkit.recipeIterator().asSequence().filter { (it as? Keyed)?.key?.namespace == "nexo" }.count() < 100) RecipesManager.reload()
        else {
            Logs.logWarn("Nexo did not reload recipes due to the number of recipes!")
            Logs.logWarn("In modern Paper-versions this would cause the server to freeze for very long times")
            Logs.logWarn("Restart your server fully for recipe-changes to take effect")
        }
        Message.RELOAD.send(sender, Placeholder.parsed("reloaded", "recipes"))
    }

    @JvmOverloads
    @JvmStatic
    fun reloadConfigs(sender: CommandSender? = Bukkit.getConsoleSender()) {
        MechanicsManager.unregisterListeners()
        MechanicsManager.unregisterTasks()
        NMSHandlers.resetHandler()
        NexoPlugin.instance().reloadConfigs()
        NexoPlugin.instance().packServer(NexoPackServer.initializeServer())
        MechanicsManager.registerNativeMechanics()
        Message.RELOAD.send(sender, tagResolver("reloaded", "configs"))
    }
}
