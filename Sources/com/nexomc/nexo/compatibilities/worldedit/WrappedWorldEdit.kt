package com.nexomc.nexo.compatibilities.worldedit

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.utils.PluginUtils
import com.sk89q.worldedit.WorldEdit
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block
import java.io.File

object WrappedWorldEdit {
    val loaded by lazy { PluginUtils.isEnabled("WorldEdit") || PluginUtils.isEnabled("FastAsyncWorldEdit") }
    val isFaweEnabled by lazy { PluginUtils.isEnabled("FastAsyncWorldEdit") }
    val handlers by lazy { WorldEditHandlers() }

    fun registerParser() {
        if (!loaded) return
        WorldEdit.getInstance().eventBus.register(handlers)
        WorldEdit.getInstance().blockFactory.register(CustomBlocksFactory())
        Bukkit.getPluginManager().registerEvents(WorldEditListener(), NexoPlugin.instance())
    }

    fun unregister() {
        if (loaded) WorldEdit.getInstance().eventBus.unregister(handlers)
    }

    fun pasteSchematic(
        loc: Location,
        schematic: File?,
        replaceBlocks: Boolean,
        shouldCopyBiomes: Boolean,
        shouldCopyEntities: Boolean
    ) {
        if (loaded) WorldEditUtils.pasteSchematic(loc, schematic!!, replaceBlocks, shouldCopyBiomes, shouldCopyEntities)
    }

    fun blocksInSchematic(loc: Location, schematic: File?, ignoreSelf: Boolean): List<Block> {
        return when {
            loaded -> WorldEditUtils.blocksInSchematic(loc, schematic!!, ignoreSelf)
            else -> listOf()
        }
    }
}
