package com.nexomc.nexo.commands

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.commands.ItemCommand.sendItemInfo
import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.items.ItemBuilder
import com.nexomc.nexo.utils.AdventureUtils.tagResolver
import com.nexomc.nexo.utils.deserialize
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.safeCast
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.entitySelectorArgumentManyPlayers
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.multiLiteralArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.stringArgument
import dev.jorel.commandapi.kotlindsl.textArgument
import java.util.concurrent.CompletableFuture
import net.kyori.adventure.audience.Audience
import org.bukkit.entity.Player
import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.getOrElse

internal fun CommandTree.inventoryCommand() = multiLiteralArgument(nodeName = "inventory", "inventory", "inv") {
    withPermission("nexo.command.inventory")
    playerExecutor { player, _ ->
        NexoPlugin.instance().invManager().itemsView(player).open(player)
    }
}

internal fun CommandTree.giveItemCommand() = literalArgument("give") {
    withPermission("nexo.command.give")
    stringArgument("item") {
        replaceSuggestions(ArgumentSuggestions.stringsAsync { info ->
            CompletableFuture.supplyAsync {
                NexoItems.unexcludedItemNames().filter { info.currentArg in it }
                    .sortedWith(compareByDescending<String> { it.startsWith(info.currentArg) }.thenBy { it }).toTypedArray()
            }
        })
        integerArgument("amount", 1, optional = true) {
            entitySelectorArgumentManyPlayers("targets", optional = true) {
                anyExecutor { sender, args ->
                    val itemID = args.get("item") as? String ?: return@anyExecutor
                    val itemBuilder = NexoItems.itemFromId(itemID) ?: return@anyExecutor Message.ITEM_NOT_FOUND.send(sender, tagResolver("item", itemID))
                    val targets = args.getOptional("targets")?.getOrElse {
                        if (sender is Player) listOf(sender) else emptyList()
                    }?.safeCast<Collection<Player>>()?.ifEmpty { null } ?: return@anyExecutor

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

internal fun CommandTree.takeItemCommand() = literalArgument("take") {
    withPermission("nexo.command.take")
    textArgument("item") {
        replaceSuggestions(ArgumentSuggestions.stringsAsync {
            CompletableFuture.supplyAsync { NexoItems.unexcludedItemNames() }
        })
        integerArgument("amount", min = 1, optional = true) {
            entitySelectorArgumentManyPlayers("players", optional = true) {
                anyExecutor { sender, args ->
                    val targets = args.getOptional("players").getOrElse {
                        if (sender is Player) listOf(sender) else emptyList()
                    }.safeCast<Collection<Player>>()?.ifEmpty { null } ?: return@anyExecutor
                    val itemID = args.get("item") as? String ?: return@anyExecutor
                    val amount = args.getOptionalByClass("amount", Int::class.java)
                    if (!NexoItems.exists(itemID))
                        return@anyExecutor Message.ITEM_NOT_FOUND.send(sender, tagResolver("item", itemID))

                    targets.forEach { target: Player ->
                        if (amount.isEmpty) target.inventory.contents.asSequence().filterNotNull()
                            .filter { !it.isEmpty && itemID == NexoItems.idFromItem(it) }
                            .forEach { target.inventory.remove(it) }
                        else {
                            var toRemove = amount.get()
                            while (toRemove > 0) {
                                target.inventory.contents.asSequence().filterNotNull()
                                    .filter { !it.isEmpty && itemID == NexoItems.idFromItem(it) }
                                    .forEach { item ->
                                        if (toRemove == 0) return@forEach
                                        toRemove -= item.amount.also { item.subtract(toRemove) }
                                        if (toRemove < 0) toRemove = 0
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
    stringArgument("itemid", true) {
        replaceSuggestions(ArgumentSuggestions.stringsAsync {
            CompletableFuture.supplyAsync { NexoItems.unexcludedItemNames() }
        })
        anyExecutor { sender, args ->
            val heldItem = (sender as? Player)?.inventory?.itemInMainHand
            val itemId = args.getOptional("itemid").getOrDefault(NexoItems.idFromItem(heldItem)) as? String ?: return@anyExecutor
            sendItemInfo(sender, NexoItems.itemFromId(itemId) ?: return@anyExecutor sender.sendMessage("<red>No item found with ID</red> <dark_red>$itemId".deserialize()), itemId)
        }
    }
}

object ItemCommand {
    internal fun sendItemInfo(sender: Audience, builder: ItemBuilder, itemId: String?) {
        sender.sendMessage("<dark_aqua>ItemID: <aqua>$itemId".deserialize())
        sender.sendMessage("<dark_green>CustomModelData: <green>${builder.nexoMeta?.customModelData}".deserialize())
        sender.sendMessage("<dark_green>Material: <green>${builder.referenceCopy().type}".deserialize())
        sender.sendMessage("<dark_green>Model Name: <green>${builder.nexoMeta?.model?.asString()}".deserialize())
        Logs.newline()
    }
}