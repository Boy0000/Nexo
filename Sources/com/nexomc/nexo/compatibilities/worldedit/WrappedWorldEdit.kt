package com.nexomc.nexo.compatibilities.worldedit

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.compatibilities.worldedit.WorldEditUtils.NexoBlockInputParser
import com.nexomc.nexo.utils.PluginUtils.isEnabled
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block
import java.io.File

object WrappedWorldEdit {
    var loaded = false

    fun init() {
        loaded = isEnabled("WorldEdit") || isEnabled("FastAsyncWorldEdit")
    }

    fun registerParser() {
        if (loaded) {
            NexoBlockInputParser()
            WorldEditHandlers(true)
            Bukkit.getPluginManager().registerEvents(WorldEditListener(), NexoPlugin.instance())
        }
    }

    fun unregister() {
        if (loaded) WorldEditHandlers(false)
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
