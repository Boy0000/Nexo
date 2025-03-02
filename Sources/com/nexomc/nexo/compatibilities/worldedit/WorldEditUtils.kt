package com.nexomc.nexo.compatibilities.worldedit

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteBlockMechanic
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteBlockMechanicFactory
import com.nexomc.nexo.utils.BlockHelpers.isReplaceable
import com.nexomc.nexo.utils.logs.Logs
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extension.input.ParserContext
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.internal.registry.InputParser
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.session.ClipboardHolder
import com.sk89q.worldedit.world.block.BaseBlock
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import java.io.File
import java.io.FileInputStream

object WorldEditUtils {
    private fun parseNoteBlock(mechanic: NoteBlockMechanic, input: String): BlockData? {
        val factory = NoteBlockMechanicFactory.instance() ?: return null
        return when {
            mechanic.isDirectional -> {
                val direction = input.substringAfter("[").substringAfter("=").substringBefore("]")
                val dirBlock = mechanic.directional

                when {
                    !dirBlock!!.isParentBlock() -> dirBlock.parentMechanic!!.blockData
                    dirBlock.isParentBlock() && direction != input -> factory.getMechanic(directionalID(mechanic, direction))!!.blockData
                    else -> mechanic.blockData
                }
            }
            else -> mechanic.blockData
        }
    }

    private fun directionalID(mechanic: NoteBlockMechanic, direction: String): String {
        val dirBlock = mechanic.directional ?: return mechanic.itemID
        return when {
            dirBlock.isLog -> when (direction) {
                "x" -> dirBlock.xBlock ?: mechanic.itemID
                "y" -> dirBlock.yBlock ?: mechanic.itemID
                "z" -> dirBlock.zBlock ?: mechanic.itemID
                else -> mechanic.itemID
            }
            else -> when (direction) {
                "north" -> dirBlock.northBlock ?: mechanic.itemID
                "south" -> dirBlock.southBlock ?: mechanic.itemID
                "west" -> dirBlock.westBlock ?: mechanic.itemID
                "east" -> dirBlock.eastBlock ?: mechanic.itemID
                "up" ->  dirBlock.upBlock?.takeIf { dirBlock.isDropper } ?: mechanic.itemID
                "down" -> dirBlock.downBlock?.takeIf { dirBlock.isDropper } ?: mechanic.itemID
                else -> mechanic.itemID
            }
        }
    }

    fun pasteSchematic(
        loc: Location,
        schematic: File,
        replaceBlocks: Boolean,
        shouldCopyBiomes: Boolean,
        shouldCopyEntities: Boolean
    ) {
        val clipboardFormat = ClipboardFormats.findByFile(schematic) ?: return
        val clipboard = runCatching {
            FileInputStream(schematic).use { inputStream ->
                clipboardFormat.getReader(inputStream).use { reader ->
                    reader.read()
                }
            }
        }.getOrNull() ?: return

        runCatching {
            val world = loc.getWorld() ?: return
            val adaptedWorld = BukkitAdapter.adapt(world)
            val editSession = WorldEdit.getInstance().newEditSessionBuilder().world(adaptedWorld).maxBlocks(-1).build()
            val operation = ClipboardHolder(clipboard).createPaste(editSession)
                .to(BlockVector3.at(loc.x, loc.y, loc.z))
                .copyBiomes(shouldCopyBiomes).copyEntities(shouldCopyEntities).ignoreAirBlocks(true).build()

            runCatching {
                if (replaceBlocks || blocksInSchematic(clipboard, loc).isEmpty()) Operations.complete(operation)
                editSession.close()
            }.onFailure {
                Logs.logWarn("Could not paste schematic for sapling-mechanic")
                if (Settings.DEBUG.toBool()) it.printStackTrace()
            }
        }.getOrThrow()
    }

    private fun blocksInSchematic(clipboard: Clipboard, loc: Location): List<Block> {
        val list = mutableListOf<Block>()
        val world = checkNotNull(loc.getWorld())
        (clipboard.minimumPoint.x..clipboard.maximumPoint.x).forEach { x ->
            (clipboard.minimumPoint.y..clipboard.maximumPoint.y).forEach { y ->
                (clipboard.minimumPoint.z..clipboard.maximumPoint.z).forEach { z ->
                    val offset = Location(
                        world,
                        (x - clipboard.origin.blockX).toDouble(),
                        (y - clipboard.origin.blockY).toDouble(),
                        (z - clipboard.origin.blockZ).toDouble()
                    )

                    val block = world.getBlockAt(loc.clone().add(offset))
                    if (isReplaceable(block) || loc.toBlockLocation() == loc.toBlockLocation()) return@forEach
                    list += block
                }
            }
        }
        return list
    }

    fun blocksInSchematic(loc: Location, schematic: File): List<Block> {
        val list = mutableListOf<Block>()
        val world = checkNotNull(loc.getWorld())
        val clipboardFormat = ClipboardFormats.findByFile(schematic) ?: return list
        val clipboard = runCatching {
            FileInputStream(schematic).use { inputStream ->
                clipboardFormat.getReader(inputStream).use { reader ->
                    reader.read()
                }
            }
        }.getOrNull() ?: return list

        (clipboard.minimumPoint.x..clipboard.maximumPoint.x).forEach { x ->
            (clipboard.minimumPoint.y..clipboard.maximumPoint.y).forEach { y ->
                (clipboard.minimumPoint.z..clipboard.maximumPoint.z)
                    .asSequence()
                    .map {
                        Location(
                            world,
                            (x - clipboard.origin.blockX).toDouble(),
                            (y - clipboard.origin.blockY).toDouble(),
                            (it - clipboard.origin.blockZ).toDouble()
                        )
                    }
                    .map { world.getBlockAt(loc.clone().add(it)) }
                    .filterTo(list) { !isReplaceable(it) && loc.toBlockLocation() != loc.toBlockLocation() }
            }
        }

        return list
    }

    class NexoBlockInputParser : InputParser<BaseBlock?>(WorldEdit.getInstance()) {
        init {
            if (WrappedWorldEdit.loaded) WorldEdit.getInstance().blockFactory.register(this)
        }

        override fun parseFromInput(input: String, context: ParserContext): BaseBlock? {
            when {
                input == "minecraft:note_block" || input == "note_block" ->
                    return BukkitAdapter.adapt(Bukkit.createBlockData(Material.NOTE_BLOCK)).toBaseBlock()
                input == "minecraft:tripwire" || input == "tripwire" ->
                    return BukkitAdapter.adapt(Bukkit.createBlockData(Material.TRIPWIRE)).toBaseBlock()
                !input.startsWith("nexo:") || input.endsWith(":") -> return null
                else -> {
                    val id = input.split(":")[1].split("\\[")[0] // Potential arguments
                    if (id == input || !NexoBlocks.isCustomBlock(id)) return null

                    val noteMechanic = NexoBlocks.noteBlockMechanic(id) ?: return null
                    val blockData = NexoBlocks.blockData(id) ?: return null

                    return when {
                        Settings.WORLDEDIT_STRINGBLOCKS.toBool() && NexoBlocks.isNexoStringBlock(id) ->
                            BukkitAdapter.adapt(blockData).toBaseBlock()
                        Settings.WORLDEDIT_NOTEBLOCKS.toBool() ->
                            BukkitAdapter.adapt(if ("[" in input) parseNoteBlock(noteMechanic, input) else blockData).toBaseBlock()
                        else -> null
                    }
                }
            }
        }
    }
}
