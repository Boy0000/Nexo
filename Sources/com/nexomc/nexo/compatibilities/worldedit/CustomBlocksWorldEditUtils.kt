package com.nexomc.nexo.compatibilities.worldedit

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic
import com.nexomc.nexo.mechanics.custom_block.CustomBlockRegistry
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.world.block.BlockStateHolder
import org.bukkit.Location
import org.jetbrains.annotations.NotNull
import javax.annotation.Nullable


object CustomBlocksWorldEditUtils {
    /**
     * The core method that processes the custom block placement or removal.
     */
    fun processBlock(location: Location, stateToSet: BlockStateHolder<*>, stateBefore: BlockStateHolder<*>) {
        // Match custom block with block state and raw place it
        getCustomBlockByBlockState(stateBefore)?.run {
            val type = CustomBlockRegistry.getByClass(this::class.java) ?: return@run
            type.removeWorldEdit(location, this)
        }

        getCustomBlockByBlockState(stateToSet)?.run {
            val type = CustomBlockRegistry.getByClass(this::class.java) ?: return@run
            type.placeWorldEdit(location, this)
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