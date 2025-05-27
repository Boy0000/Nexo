package com.nexomc.nexo.compatibilities.worldedit

import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteBlockMechanic
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteBlockMechanicFactory
import com.nexomc.nexo.utils.BlockHelpers
import com.nexomc.nexo.utils.logs.Logs
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.session.ClipboardHolder
import org.bukkit.Location
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
        copyBiomes: Boolean,
        copyEntities: Boolean
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
            val world = BukkitAdapter.adapt(loc.world ?: return)
            val editSession = WorldEdit.getInstance().newEditSessionBuilder().world(world).maxBlocks(-1).build()
            val operation = ClipboardHolder(clipboard).createPaste(editSession)
                .to(BlockVector3.at(loc.x, loc.y, loc.z))
                .copyBiomes(copyBiomes).copyEntities(copyEntities).ignoreAirBlocks(true).build()

            runCatching {
                if (replaceBlocks || blocksInSchematic(clipboard, loc).isEmpty()) Operations.complete(operation)
                editSession.close()
            }.onFailure {
                Logs.logWarn("Could not paste schematic for sapling-mechanic")
                if (Settings.DEBUG.toBool()) it.printStackTrace()
            }
        }.onFailure {
            Logs.logWarn("Could not paste schematic")
            if (Settings.DEBUG.toBool()) it.printStackTrace()
        }
    }

    private fun blocksInSchematic(clipboard: Clipboard, loc: Location): List<Block> {
        val list = mutableListOf<Block>()
        val world = loc.world ?: return emptyList()
        (clipboard.minimumPoint.x()..clipboard.maximumPoint.x()).forEach { x ->
            (clipboard.minimumPoint.y()..clipboard.maximumPoint.y()).forEach { y ->
                (clipboard.minimumPoint.z()..clipboard.maximumPoint.z()).forEach { z ->
                    val x = (x - clipboard.minimumPoint.x()).toDouble()
                    val y = (y - clipboard.minimumPoint.y()).toDouble()
                    val z = (z - clipboard.minimumPoint.z()).toDouble()

                    val block = world.getBlockAt(loc.clone().add(x, y, z))
                    if (BlockHelpers.isReplaceable(block)) return@forEach
                    list += block
                }
            }
        }
        return list
    }

    fun blocksInSchematic(loc: Location, schematic: File, ignoreSelf: Boolean): List<Block> {
        val list = mutableListOf<Block>()
        val world = loc.world ?: return emptyList()
        val clipboardFormat = ClipboardFormats.findByFile(schematic) ?: return list
        val clipboard = runCatching {
            FileInputStream(schematic).use { inputStream ->
                clipboardFormat.getReader(inputStream).use { reader ->
                    reader.read()
                }
            }
        }.getOrNull() ?: return list

        (clipboard.minimumPoint.x()..clipboard.maximumPoint.x()).forEach { x ->
            (clipboard.minimumPoint.y()..clipboard.maximumPoint.y()).forEach { y ->
                (clipboard.minimumPoint.z()..clipboard.maximumPoint.z()).forEach { z ->
                    val x = (x - clipboard.minimumPoint.x()).toDouble()
                    val y = (y - clipboard.minimumPoint.y()).toDouble()
                    val z = (z - clipboard.minimumPoint.z()).toDouble()

                    val block = world.getBlockAt(loc.clone().add(x, y, z))
                    if (BlockHelpers.isReplaceable(block)) return@forEach
                    list += block
                }
            }
        }

        list -= loc.block

        return list
    }
}
