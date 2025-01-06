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
import com.nexomc.nexo.mechanics.furniture.FurnitureFactory
import com.nexomc.nexo.nms.NMSHandlers.resetHandler
import com.nexomc.nexo.pack.PackGenerator
import com.nexomc.nexo.pack.server.NexoPackServer.Companion.initializeServer
import com.nexomc.nexo.recipes.RecipesManager
import com.nexomc.nexo.utils.AdventureUtils.tagResolver
import com.nexomc.nexo.utils.flatMapFast
import com.nexomc.nexo.utils.logs.Logs
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.multiLiteralArgument
import dev.jorel.commandapi.kotlindsl.textArgument
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
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
                "configs" -> {
                    MechanicsManager.unregisterListeners()
                    MechanicsManager.unregisterTasks()
                    NexoPlugin.instance().reloadConfigs()
                    MechanicsManager.registerNativeMechanics()
                }
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
        MechanicsManager.unregisterListeners()
        MechanicsManager.unregisterTasks()
        resetHandler()
        NexoPlugin.instance().reloadConfigs()
        NexoPlugin.instance().packServer(initializeServer())
        MechanicsManager.registerNativeMechanics()
        reloadItems(sender)
        reloadRecipes(sender)
        reloadPack(sender)
    }

    @JvmOverloads
    @JvmStatic
    fun reloadItems(sender: CommandSender? = Bukkit.getConsoleSender()) {
        Message.RELOAD.send(sender, tagResolver("reloaded", "items"))
        NexoItems.itemConfigCache.clear()
        FurnitureFactory.instance()?.packetManager()?.removeAllFurniturePackets()
        NexoItems.loadItems()
        NexoPlugin.instance().invManager().regen()

        if (Settings.UPDATE_ITEMS.toBool() && Settings.UPDATE_ITEMS_ON_RELOAD.toBool()) {
            Logs.logInfo("Updating all items in player-inventories...")
            Bukkit.getScheduler().runTaskAsynchronously(NexoPlugin.instance(), Runnable {
                Bukkit.getServer().onlinePlayers.forEach { player ->
                    val updates = ObjectArrayList<Pair<Int, ItemStack>>()

                    player.inventory.contents.forEachIndexed { index, item ->
                        if (item == null) return@forEachIndexed
                        val newItem = ItemUpdater.updateItem(item).takeUnless { it == item } ?: return@forEachIndexed
                        updates.add(index to newItem)
                    }

                    Bukkit.getScheduler().runTask(NexoPlugin.instance(), Runnable {
                        updates.forEach { (index, newItem) ->
                            player.inventory.setItem(index, newItem)
                        }
                    })
                }
            })
        }

        Logs.logInfo("Updating all placed furniture...")
        for (baseEntity in Bukkit.getWorlds().flatMapFast { it.getEntitiesByClass(ItemDisplay::class.java) })
            NexoFurniture.updateFurniture(baseEntity)
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
        Message.RELOAD.send(sender, tagResolver("reloaded", "recipes"))
        RecipesManager.reload()
    }
}
