package com.nexomc.nexo.compatibilities.worldedit

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.utils.PluginUtils.isEnabled
import com.sk89q.worldedit.WorldEdit
import java.io.File
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block

object WrappedWorldEdit {
    var loaded = false
    var isFaweEnabled = false
        private set

    val handlers by lazy { WorldEditHandlers() }

    fun init() {
        loaded = isEnabled("WorldEdit") || isEnabled("FastAsyncWorldEdit")
        isFaweEnabled = isEnabled("FastAsyncWorldEdit")
    }

    fun registerParser() {
        if (loaded) {
            WorldEdit.getInstance().eventBus.register(handlers)
            WorldEdit.getInstance().blockFactory.register(CustomBlocksFactory())
            Bukkit.getPluginManager().registerEvents(WorldEditListener(), NexoPlugin.instance())
        }
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

    fun blocksInSchematic(loc: Location, schematic: File?): List<Block> {
        return if (loaded) WorldEditUtils.blocksInSchematic(loc, schematic!!)
        else listOf()
    }
}
