package com.nexomc.nexo.compatibilities.worldedit

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.world.block.BlockStateHolder
import javax.annotation.Nullable
import org.bukkit.Location
import org.jetbrains.annotations.NotNull


object CustomBlocksWorldEditUtils {
    /**
     * The core method that processes the custom block placement or removal.
     */
    fun processBlock(location: Location, stateToSet: BlockStateHolder<*>) {
        // Match custom block with block state and raw place it
        val stateToSetType = stateToSet.blockType
        if (CustomBlocksFactory.isCustomBlockType(stateToSetType)) {
            val customBlock = getCustomBlockByBlockState(stateToSet) ?: return
            NexoBlocks.place(customBlock.itemID, location)
        }
    }

    /**
     * Get the custom block by the block state.
     */
    @Nullable
    private fun getCustomBlockByBlockState(@NotNull blockState: BlockStateHolder<*>): CustomBlockMechanic? {
        return NexoBlocks.customBlockMechanic(BukkitAdapter.adapt(blockState.toImmutableState()))
    }
}