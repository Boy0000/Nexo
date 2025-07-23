package com.nexomc.nexo.mechanics.custom_block.noteblock

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.utils.BlockHelpers.toCenterBlockLocation
import com.nexomc.nexo.utils.drops.Drop
import org.bukkit.Instrument
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Note
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.type.NoteBlock
import org.bukkit.entity.FallingBlock
import org.bukkit.persistence.PersistentDataType

object NoteMechanicHelpers {

    @JvmStatic
    fun checkNoteBlockAbove(loc: Location) {
        val block = loc.block.getRelative(BlockFace.UP)
        if (block.type == Material.NOTE_BLOCK) block.state.update(true, true)
        val nextBlock = loc.block.getRelative(BlockFace.UP, 2)
        if (nextBlock.type == Material.NOTE_BLOCK) checkNoteBlockAbove(block.location)
    }

    /**
    We have 16 instruments with 25 notes. All of those blocks can be powered.
     That's: 16*25*2 = 800 variations. The first 25 variations of PIANO (not powered)
     will be reserved for the vanilla behavior. We still have 800-25 = 775 variations
     */
    fun legacyBlockData(customVariation: Int): NoteBlock? {
        return (Material.NOTE_BLOCK.createBlockData() as NoteBlock).also {
            it.instrument = Instrument.getByType((((customVariation + 26) % 400) / 25).toByte()) ?: return null
            it.note = Note(customVariation % 25)
            it.isPowered = (customVariation >= 400)
        }
    }

    fun modernBlockData(customVariation: Int): NoteBlock? {
        return (Material.NOTE_BLOCK.createBlockData() as NoteBlock).also {
            it.instrument = Instrument.getByType(Instrument.entries.size.coerceAtMost(customVariation / 50).toByte()) ?: return null
            it.note = Note((customVariation % 25))
            it.isPowered = (customVariation % 50 >= 25)
        }
    }

    fun modernCustomVariation(noteBlock: NoteBlock): Int? {
        val instrumentIndex = Instrument.entries.indexOf(noteBlock.instrument).takeUnless { it == -1 } ?: return null
        return (instrumentIndex * 50) + (noteBlock.note.id) + if (noteBlock.isPowered) 25 else 0
    }


    fun handleFallingNexoBlockAbove(block: Block) {
        val blockAbove = block.getRelative(BlockFace.UP)
        val mechanic = NexoBlocks.noteBlockMechanic(blockAbove)?.takeIf { it.isFalling() } ?: return
        val fallingLocation = toCenterBlockLocation(blockAbove.location)
        val fallingData = NexoBlocks.blockData(mechanic.itemID) ?: return
        NexoBlocks.remove(blockAbove.location, overrideDrop = Drop.emptyDrop())

        val falling = blockAbove.world.spawn(fallingLocation, FallingBlock::class.java)
        falling.blockData = fallingData!!
        falling.persistentDataContainer.set(NoteBlockMechanic.FALLING_KEY, PersistentDataType.BYTE, 1)
        handleFallingNexoBlockAbove(blockAbove)
    }
}
