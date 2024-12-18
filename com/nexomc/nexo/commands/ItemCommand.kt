package com.nexomc.nexo.commands

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.commands.ItemCommand.sendItemInfo
import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.items.ItemBuilder
import com.nexomc.nexo.utils.AdventureUtils
import com.nexomc.nexo.utils.AdventureUtils.tagResolver
import com.nexomc.nexo.utils.ItemUtils
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.safeCast
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.kotlindsl.*
import io.lumine.mythic.bukkit.utils.lib.jooq.impl.QOM.Ne
import net.kyori.adventure.audience.Audience
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

internal fun CommandTree.inventoryCommand() = multiLiteralArgument(nodeName = "inventory", "inventory", "inv") {
    withPermission("nexo.command.inventory")
    playerExecutor { player, _ ->
        NexoPlugin.instance().invManager().itemsView(player).open(player)
    }
}

internal fun CommandTree.giveItemCommand() = literalArgument("give") {
    withPermission("nexo.command.give")
    entitySelectorArgumentManyPlayers("targets") {
        stringArgument("item") {
            replaceSuggestions(ArgumentSuggestions.stringsAsync {
                CompletableFuture.supplyAsync { NexoItems.itemNames() }
            })
            integerArgument("amount", 1, optional = true) {
                anyExecutor { sender, args ->
                    val targets = args.get("targets").safeCast<Collection<Player>>() ?: return@anyExecutor
                    val itemID = args.get("item") as? String ?: return@anyExecutor
                    val itemBuilder = NexoItems.itemFromId(itemID)
                        ?: return@anyExecutor Message.ITEM_NOT_FOUND.send(sender, tagResolver("item", itemID))

                    var amount = args.getOptionalByClass("amount", Int::class.java).orElse(1)
                    val maxAmount = itemBuilder.maxStackSize ?: itemBuilder.type.maxStackSize
                    val slots = amount / maxAmount + (if (maxAmount % amount > 0) 1 else 0)
                    val items = itemBuilder.buildArray(if (slots > 36) ((maxAmount * 36).also { amount = it }) else amount).filterNotNull()

                    targets.forEach {
                        val output = it.inventory.addItem(*items.toTypedArray())
                        output.values.forEach { stack ->
                            it.world.dropItem(it.location, stack)
                        }
                    }
                    when (targets.size) {
                        1 -> Message.GIVE_PLAYER.send(
                            sender, tagResolver("player", (targets.iterator().next().name)),
                            tagResolver("amount", amount.toString()),
                            tagResolver("item", itemID)
                        )

                        else -> Message.GIVE_PLAYERS.send(
                            sender, tagResolver("count", targets.size.toString()),
                            tagResolver("amount", amount.toString()),
                            tagResolver("item", itemID)
                        )
                    }
                }
            }
        }
    }
}



/*fun CommandTree.simpleGiveItemCommand() = literalArgument("give") {
    withPermission("nexo.command.give")
    entitySelectorArgumentManyPlayers("players") {
        textArgument("item") {
            replaceSuggestions(ArgumentSuggestions.strings(*NexoItems.itemNames()))
            anyExecutor { sender, args ->
                val targets = args.get("players") as? Collection<Player> ?: return@anyExecutor
                val itemID = args.get("itemID") as? String ?: return@anyExecutor
                val itemBuilder = NexoItems.itemById(itemID)
                    ?: return@anyExecutor Message.ITEM_NOT_FOUND.send(sender, tagResolver("item", itemID))

                targets.forEach { target ->
                    target.inventory.addItem(ItemUpdater.updateItem(itemBuilder.build()))
                }
                when (targets.size) {
                    1 -> Message.GIVE_PLAYER.send(
                        sender, tagResolver("player", targets.iterator().next().name),
                        tagResolver("amount", java.lang.String.valueOf(1)),
                        tagResolver("item", itemID)
                    )

                    else -> Message.GIVE_PLAYERS.send(
                        sender, tagResolver("count", java.lang.String.valueOf(targets.size)),
                        tagResolver("amount", java.lang.String.valueOf(1)),
                        tagResolver("item", itemID)
                    )
                }
            }
        }
    }
}*/

internal fun CommandTree.takeItemCommand() = literalArgument("take") {
    withPermission("nexo.command.take")
    entitySelectorArgumentManyPlayers("players") {
        textArgument("item") {
            replaceSuggestions(ArgumentSuggestions.stringsAsync {
                CompletableFuture.supplyAsync { NexoItems.itemNames() }
            })
            integerArgument("amount", min = 1, optional = true) {
                anyExecutor { sender, args ->
                    val targets = args.get("targets").safeCast<Collection<Player>>() ?: return@anyExecutor
                    val itemID = args.get("item") as? String ?: return@anyExecutor
                    val amount = args.getOptionalByClass("amount", Int::class.java)
                    if (!NexoItems.exists(itemID))
                        return@anyExecutor Message.ITEM_NOT_FOUND.send(sender, tagResolver("item", itemID))

                    targets.forEach { target: Player ->
                        if (amount.isEmpty) target.inventory.contents.asSequence().filterNotNull()
                            .filter { !ItemUtils.isEmpty(it) && itemID == NexoItems.idFromItem(it) }
                            .forEach { target.inventory.remove(it) }
                        else {
                            var toRemove = amount.get()
                            while (toRemove > 0) {
                                target.inventory.contents.asSequence().filterNotNull()
                                    .filter { !ItemUtils.isEmpty(it) && itemID == NexoItems.idFromItem(it) }
                                    .forEach {
                                        if (toRemove == 0) return@forEach
                                        when {
                                            it.amount <= toRemove -> {
                                                toRemove -= it.amount
                                                target.inventory.remove(it)
                                            }

                                            else -> {
                                                it.amount -= toRemove
                                                toRemove = 0
                                            }
                                        }
                                    }

                                if (toRemove > 0) break
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun CommandTree.itemInfoCommand() = literalArgument("iteminfo") {
    withPermission("nexo.command.iteminfo")
    stringArgument("itemid") {
        replaceSuggestions(ArgumentSuggestions.stringsAsync {
            CompletableFuture.supplyAsync { NexoItems.itemNames() }
        })
        anyExecutor { sender, args ->
            val argument = args.get("itemid") as? String ?: return@anyExecutor
            val audience = NexoPlugin.instance().audience().sender(sender)
            when (argument) {
                "all" -> for ((itemId, builder) in NexoItems.entries()) sendItemInfo(audience, builder, itemId)
                else -> sendItemInfo(
                    audience,
                    NexoItems.itemFromId(argument)
                        ?: return@anyExecutor audience.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<red>No item found with ID</red> <dark_red>$argument")),
                    argument
                )
            }
        }
    }
}

object ItemCommand {
    internal fun sendItemInfo(sender: Audience, builder: ItemBuilder, itemId: String?) {
        sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_aqua>ItemID: <aqua>$itemId"))
        sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_green>CustomModelData: <green>${builder.nexoMeta?.customModelData}"))
        sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_green>Material: <green>${builder.referenceCopy().type}"))
        sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_green>Model Name: <green>${builder.nexoMeta?.modelKey?.asString()}"))
        Logs.newline()
    }
}