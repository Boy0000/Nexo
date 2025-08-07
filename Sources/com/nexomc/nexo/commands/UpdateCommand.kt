package com.nexomc.nexo.commands

import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.items.ItemUpdater
import com.nexomc.nexo.utils.AdventureUtils.tagResolver
import com.nexomc.nexo.utils.safeCast
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.doubleArgument
import dev.jorel.commandapi.kotlindsl.entitySelectorArgumentManyPlayers
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player

internal fun CommandTree.updateCommand() = literalArgument("update") {
    withPermission("nexo.command.update")
    literalArgument("item") {
        entitySelectorArgumentManyPlayers("players", optional = true) {
            anyExecutor { commandSender, args ->
                args.getOptional("players").map { it.safeCast<List<Player>>() }
                    .orElse(listOf(commandSender).filterIsInstance<Player>())?.forEach { p ->
                        var updated = 0
                        (0.. p.inventory.size).forEach { i ->
                            val oldItem = p.inventory.getItem(i) ?: return@forEach
                            val newItem = ItemUpdater.updateItem(oldItem).takeUnless(oldItem::equals) ?: return@forEach
                            p.inventory.setItem(i, newItem)
                            updated++
                        }
                        p.updateInventory()
                        Message.UPDATED_ITEMS.send(
                            p, tagResolver("amount", updated.toString()),
                            tagResolver("player", p.displayName)
                        )
                    }
            }
        }
    }
    literalArgument("furniture") {
        doubleArgument("radius", optional = true) {
            playerExecutor { player, args ->
                val radius = args.getOptional("radius").orElse(10.0) as Double
                player.getNearbyEntities(radius, radius, radius).filterIsInstance<ItemDisplay>().forEach(NexoFurniture::updateFurniture)
            }
        }
    }
}
