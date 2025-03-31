package com.nexomc.nexo.commands

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.commands.BlockInfoCommand.sendBlockInfo
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteBlockMechanicFactory
import com.nexomc.nexo.mechanics.custom_block.stringblock.StringBlockMechanicFactory
import com.nexomc.nexo.utils.AdventureUtils
import com.nexomc.nexo.utils.logs.Logs
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.stringArgument
import java.util.concurrent.CompletableFuture
import org.bukkit.command.CommandSender

internal fun CommandTree.blockInfoCommand() = literalArgument("blockinfo") {
    withPermission("nexo.command.blockinfo")
    stringArgument("itemid") {
        replaceSuggestions(ArgumentSuggestions.stringsAsync {
            CompletableFuture.supplyAsync { NexoBlocks.blockIDs() }
        })
        anyExecutor { sender, args ->
            val argument = args.get("itemid") as? String ?: return@anyExecutor
            when {
                argument == "all" -> NexoItems.itemNames().asSequence().filter(NexoBlocks::isCustomBlock).forEach { sendBlockInfo(sender, it) }
                NexoItems.itemFromId(argument) == null -> sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<red>No block found with ID</red> <dark_red>$argument"))
                else -> sendBlockInfo(sender, argument)
            }
        }
    }
}

object BlockInfoCommand {


    internal fun sendBlockInfo(sender: CommandSender, itemId: String) {
        sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_aqua>ItemID: <aqua>$itemId"))
        when {
            NexoBlocks.isNexoNoteBlock(itemId) -> {
                val data = NoteBlockMechanicFactory.instance()?.getMechanic(itemId)?.blockData ?: return
                sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_aqua>Instrument: ${data.instrument}"))
                sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_aqua>Note: ${data.note.id}"))
                sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_aqua>Powered: ${data.isPowered}"))
            }
            NexoBlocks.isNexoStringBlock(itemId) -> {
                val data = StringBlockMechanicFactory.instance()?.getMechanic(itemId)?.blockData ?: return
                sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_aqua>Facing: ${data.faces}"))
                sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_aqua>Powered: ${data.isPowered}"))
                sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_aqua>Disarmed: ${data.isDisarmed}"))
            }
        }
        Logs.newline()
    }
}
