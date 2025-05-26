package com.nexomc.nexo.compatibilities.worldedit

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extension.input.ParserContext
import com.sk89q.worldedit.internal.registry.InputParser
import com.sk89q.worldedit.world.block.BaseBlock
import com.sk89q.worldedit.world.block.BlockType
import com.sk89q.worldedit.world.block.BlockTypes
import java.util.stream.Stream


class CustomBlocksFactory : InputParser<BaseBlock?>(WorldEdit.getInstance()) {
    override fun getSuggestions(input: String): Stream<String> {
        return NexoBlocks.blockIDs().filter { input in it }.stream()
    }

    override fun parseFromInput(input: String, context: ParserContext?): BaseBlock? {
        val blockId = input.removePrefix("nexo:")
        val customBlock = NexoBlocks.customBlockMechanic(blockId) ?: return null

        return createBaseBlockFromCustomBlock(customBlock)
    }

    /**
     * Creates a worldedit [BaseBlock] from a custom block.
     */
    private fun createBaseBlockFromCustomBlock(customBlock: CustomBlockMechanic): BaseBlock {
        return BukkitAdapter.adapt(customBlock.blockData!!).toBaseBlock()
    }

    companion object {
        fun isCustomBlockType(blockType: BlockType?): Boolean {
            return blockType === BlockTypes.NOTE_BLOCK || blockType === BlockTypes.CHORUS_PLANT || blockType === BlockTypes.TRIPWIRE
        }
    }
}