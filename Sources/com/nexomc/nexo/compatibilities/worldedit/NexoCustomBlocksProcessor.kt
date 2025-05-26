package com.nexomc.nexo.compatibilities.worldedit

import com.fastasyncworldedit.core.extent.processor.ProcessorScope
import com.fastasyncworldedit.core.queue.IBatchProcessor
import com.fastasyncworldedit.core.queue.IChunk
import com.fastasyncworldedit.core.queue.IChunkGet
import com.fastasyncworldedit.core.queue.IChunkSet
import com.sk89q.worldedit.extent.Extent
import com.sk89q.worldedit.world.block.BlockTypesCache
import org.bukkit.Location
import org.bukkit.World
import javax.annotation.Nullable


class NexoCustomBlocksProcessor(val world: World) : IBatchProcessor {

    override fun processSet(chunk: IChunk?, get: IChunkGet?, set: IChunkSet?): IChunkSet? {
        return null
    }

    override fun postProcess(chunk: IChunk, get: IChunkGet, set: IChunkSet) {
        val (bx, bz) = (chunk.x shl 4) to (chunk.z shl 4)

        for (layer in get.minSectionPosition..get.maxSectionPosition) {
            if (!set.hasSection(layer)) continue

            // loadIfPresent shouldn't be null if set.hasSection(layer) is true
            val (blocksSet, blocksGet) = set.loadIfPresent(layer)!! to get.loadIfPresent(layer)!!

            // Account for negative layers
            val by = layer shl 4
            var y = 0
            var index = 0
            while (y < 16) {
                val yy = y + by
                for (z in 0..15) {
                    val zz = z + bz
                    var x = 0
                    while (x < 16) {
                        val rawStateSet = blocksSet[index].code
                        if (rawStateSet == BlockTypesCache.ReservedIDs.__RESERVED__) {
                            x++
                            index++
                            continue
                        }
                        val rawStateGet = blocksGet[index].code

                        // If they are the same, skip
                        if (rawStateSet == rawStateGet) {
                            x++
                            index++
                            continue
                        }

                        val xx = bx + x

                        val (stateSet, stateGet) = BlockTypesCache.states[rawStateSet] to BlockTypesCache.states[rawStateGet]

                        val location = Location(this.world, xx.toDouble(), yy.toDouble(), zz.toDouble())

                        CustomBlocksWorldEditUtils.processBlock(location, stateSet, stateGet)
                        x++
                        index++
                    }
                }
                y++
            }
        }
    }

    @Nullable
    override fun construct(child: Extent): Extent {
        return NexoWorldEditExtent(child, this.world)
    }

    override fun getScope(): ProcessorScope {
        return ProcessorScope.READING_SET_BLOCKS
    }
}