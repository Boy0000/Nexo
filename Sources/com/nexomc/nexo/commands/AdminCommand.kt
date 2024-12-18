package com.nexomc.nexo.commands

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.utils.AdventureUtils
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.*
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.random.Random

object AdminCommand {
    fun adminCommand(): CommandAPICommand {
        return CommandAPICommand("admin")
            .withPermission("nexo.command.admin")
            .withSubcommands(furniturePlaceRemoveCommand, noteblockPlaceRemoveCommand)
    }

    private val noteblockPlaceRemoveCommand: CommandAPICommand
        get() = CommandAPICommand("block")
            .withArguments(TextArgument("block").replaceSuggestions(ArgumentSuggestions.stringsAsync {
                CompletableFuture.supplyAsync { NexoBlocks.blockIDs().toTypedArray() }
            }))
            .withArguments(TextArgument("type").replaceSuggestions(ArgumentSuggestions.strings("place", "remove")))
            .withOptionalArguments(LocationArgument("location"))
            .withOptionalArguments(IntegerArgument("radius"))
            .withOptionalArguments(BooleanArgument("random"))
            .executesPlayer(PlayerCommandExecutor { player: Player, args: CommandArguments ->
                val id = args.get("block") as String?
                if (!NexoBlocks.isCustomBlock(id)) return@PlayerCommandExecutor NexoPlugin.instance().audience().player(player)
                    .sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<red>Unknown NexoBlock: <yellow>$id"))

                val loc = args.getOptional("location").orElse(player.location) as Location
                val type = args.get("type") as String?
                val radius = args.getOptional("radius").orElse(1) as Int
                val isRandom = args.getOptional("random").orElse(false) as Boolean
                getBlocks(loc, radius, isRandom).forEach { block ->
                    if (type == null) return@forEach
                    if (type == "remove") NexoBlocks.remove(block.location)
                    if (type == "place") NexoBlocks.place(id, block.location)
                }
            })

    private val furniturePlaceRemoveCommand: CommandAPICommand
        get() {
            return CommandAPICommand("furniture")
                .withArguments(
                    TextArgument("type").replaceSuggestions(ArgumentSuggestions.strings("place", "remove")),
                    TextArgument("furniture").replaceSuggestions(ArgumentSuggestions.stringsAsync {
                        CompletableFuture.supplyAsync { NexoFurniture.furnitureIDs().plus("all").toTypedArray() }
                    })
                )
                .withOptionalArguments(
                    LocationArgument("location"),
                    IntegerArgument("radius"),
                    BooleanArgument("random")
                )
                .executesPlayer(PlayerCommandExecutor { player: Player, args: CommandArguments ->
                    val type = checkNotNull(args.get("type") as String?)
                    val id = args.getOrDefault("furniture", "") as String
                    if (!NexoFurniture.isFurniture(id)) NexoPlugin.instance().audience().player(player)
                        .sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<red>Unknown Furniture: <yellow>$id"))
                    else {
                        val loc = args.getOptional("location").orElse(player.location) as Location
                        val radius = args.getOptional("radius").orElse(0) as Int
                        val isRandom = args.getOptional("random").orElse(false) as Boolean
                        for (block: Block in getBlocks(loc, radius, isRandom)) {
                            if (type == "remove") {
                                val mechanic = NexoFurniture.furnitureMechanic(block.location)
                                if (mechanic != null && (id.isEmpty() || id == "all" || mechanic.itemID == id))
                                    NexoFurniture.remove(block.location)
                            }
                            if (type == "place") NexoFurniture.place(id, block.location, 0f, BlockFace.NORTH)
                        }
                    }
                })
        }

    private fun getBlocks(loc: Location, radius: Int, isRandom: Boolean): Collection<Block> {
        val blocks = mutableListOf<Block>()
        if (radius <= 0) return Collections.singletonList(loc.block)
        for (x in loc.blockX - radius..loc.blockX + radius) for (z in loc.blockZ - radius..loc.blockZ + radius) for (y in loc.blockY - radius..loc.blockY + radius) {
            blocks += loc.getWorld().getBlockAt(x, y, z)
        }
        if (isRandom) return Collections.singletonList(blocks[Random.nextInt(blocks.size)])
        return blocks
    }
}
