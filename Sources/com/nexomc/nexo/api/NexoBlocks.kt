package com.nexomc.nexo.api

import com.nexomc.nexo.mechanics.custom_block.CustomBlockFactory
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic
import com.nexomc.nexo.mechanics.custom_block.CustomBlockRegistry
import com.nexomc.nexo.mechanics.custom_block.chorusblock.ChorusBlockFactory
import com.nexomc.nexo.mechanics.custom_block.chorusblock.ChorusBlockMechanic
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteBlockMechanic
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteBlockMechanicFactory
import com.nexomc.nexo.mechanics.custom_block.stringblock.StringBlockMechanic
import com.nexomc.nexo.mechanics.custom_block.stringblock.StringBlockMechanicFactory
import com.nexomc.nexo.utils.drops.Drop
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object NexoBlocks {
    /**
     * Get all NexoItem ID's that have either a NoteBlockMechanic or a StringBlockMechanic
     *
     * @return A set of all NexoItem ID's that have either a NoteBlockMechanic or a StringBlockMechanic
     */
    @JvmStatic
    fun blockIDs(): Array<String> = NexoItems.itemNames().filter(::isCustomBlock).toTypedArray()

    /**
     * Get all NexoItem ID's that have a NoteBlockMechanic
     *
     * @return A set of all NexoItem ID's that have a NoteBlockMechanic
     */
    @JvmStatic
    fun noteBlockIDs(): Array<String> = NexoItems.itemNames().filter(::isNexoNoteBlock).toTypedArray()

    /**
     * Get all NexoItem ID's that have a StringBlockMechanic
     *
     * @return A set of all NexoItem ID's that have a StringBlockMechanic
     */
    @JvmStatic
    fun stringBlockIDs(): Array<String> = NexoItems.itemNames().filter(::isNexoStringBlock).toTypedArray()

    /**
     * Get all NexoItem ID's that have a ChorusBlockMechanic
     *
     * @return A set of all NexoItem ID's that have a ChorusBlockMechanic
     */
    @JvmStatic
    fun chorusBlockIDs(): Array<String>  = NexoItems.itemNames().filter(::isNexoChorusBlock).toTypedArray()

    /**
     * Check if a block is an instance of a NexoBlock
     *
     * @param block The block to check
     * @return true if the block is an instance of a NexoBlock, otherwise false
     */
    @JvmStatic
    fun isCustomBlock(block: Block?): Boolean {
        return when (block?.type) {
            Material.NOTE_BLOCK -> noteBlockMechanic(block)
            Material.TRIPWIRE -> stringMechanic(block)
            Material.CHORUS_PLANT -> chorusBlockMechanic(block)
            else -> block?.let(CustomBlockRegistry::getMechanic)
        } != null
    }

    @JvmStatic
    fun isCustomBlock(itemStack: ItemStack?) = isCustomBlock(NexoItems.idFromItem(itemStack))

    /**
     * Check if an itemID is an instance of an NexoBlock
     *
     * @param itemId The ID to check
     * @return true if the itemID is an instance of an NexoBlock, otherwise false
     */
    @JvmStatic
    fun isCustomBlock(itemId: String?) = CustomBlockFactory.instance()?.getMechanic(itemId) != null

    /**
     * Check if a block is an instance of a NoteBlock
     *
     * @param block The block to check
     * @return true if the block is an instance of a NoteBlock, otherwise false
     */
    @JvmStatic
    fun isNexoNoteBlock(block: Block) = block.type == Material.NOTE_BLOCK && noteBlockMechanic(block) != null

    /**
     * Check if an itemID has a NoteBlockMechanic
     *
     * @param itemID The itemID to check
     * @return true if the itemID has a NoteBlockMechanic, otherwise false
     */
    @JvmStatic
    fun isNexoNoteBlock(itemID: String?) = NoteBlockMechanicFactory.instance()?.isNotImplementedIn(itemID) == false

    @JvmStatic
    fun isNexoNoteBlock(item: ItemStack?) = isNexoNoteBlock(NexoItems.idFromItem(item))

    /**
     * Check if a block is an instance of a StringBlock
     *
     * @param block The block to check
     * @return true if the block is an instance of a StringBlock, otherwise false
     */
    @JvmStatic
    fun isNexoStringBlock(block: Block) = block.type == Material.TRIPWIRE && stringMechanic(block) != null

    /**
     * Check if an itemID has a StringBlockMechanic
     *
     * @param itemID The itemID to check
     * @return true if the itemID has a StringBlockMechanic, otherwise false
     */
    @JvmStatic
    fun isNexoStringBlock(itemID: String?) = StringBlockMechanicFactory.instance()?.isNotImplementedIn(itemID) == false

    /**
     * Check if a block is an instance of a ChorusBlock
     *
     * @param block The block to check
     * @return true if the block is an instance of a ChorusBlock, otherwise false
     */
    @JvmStatic
    fun isNexoChorusBlock(block: Block) = block.type == Material.CHORUS_PLANT && chorusBlockMechanic(block) != null

    /**
     * Check if an itemID has a ChorusBlockMechanic
     *
     * @param itemID The itemID to check
     * @return true if the itemID has a ChorusBlockMechanic, otherwise false
     */
    @JvmStatic
    fun isNexoChorusBlock(itemID: String?) = ChorusBlockFactory.instance()?.isNotImplementedIn(itemID) == false

    @JvmStatic
    fun place(itemID: String?, location: Location) {
        val mechanic = itemID?.let(CustomBlockRegistry::getMechanic) ?: return
        val type = CustomBlockRegistry.get(mechanic.factory?.mechanicID) ?: return
        type.placeCustomBlock(location, itemID)
    }

    /**
     * Get the BlockData assosiated with
     *
     * @param itemID The ItemID of the NexoBlock
     * @return The BlockData assosiated with the ItemID, can be null
     */
    @JvmStatic
    fun blockData(itemID: String?): BlockData? {
        val mechanic = itemID?.let(CustomBlockRegistry::getMechanic) ?: return null
        return mechanic.blockData
    }

    /**
     * Breaks a NexoBlock at the given location
     *
     * @param location  The location of the NexoBlock
     * @param player    The player that broke the block can be null
     * @param forceDrop Whether to force the block to drop, even if the player is null or in creative mode
     * @return True if the block was broken, false if the block was not a NexoBlock or could not be broken
     */
    @JvmStatic
    fun remove(location: Location, player: Player? = null, forceDrop: Boolean): Boolean {
        val overrideDrop = if (!forceDrop) null else customBlockMechanic(location)?.breakable?.drop
        return remove(location, player, overrideDrop)
    }

    /**
     * Breaks a NexoBlock at the given location
     *
     * @param location The location of the NexoBlock
     * @param player   The player that broke the block can be null
     * @return True if the block was broken, false if the block was not a NexoBlock or could not be broken
     */
    @JvmOverloads
    @JvmStatic
    fun remove(location: Location, player: Player? = null, overrideDrop: Drop? = null): Boolean {
        val block = location.block
        val mechanic = CustomBlockRegistry.getMechanic(block) ?: return false
        val type = CustomBlockRegistry.getByClass(mechanic::class.java) ?: return false
        return type.removeCustomBlock(block, player, overrideDrop)
    }

    /**
     * Get the NexoBlock at a location
     *
     * @param location The location to check
     * @return The Mechanic of the NexoBlock at the location, or null if there is no NexoBlock at the location.
     * Keep in mind that this method returns the base Mechanic, not the type. Therefore, you will need to cast this to the type you need
     */
    @JvmStatic
    fun customBlockMechanic(location: Location): CustomBlockMechanic? {
        return CustomBlockRegistry.getMechanic(location.block)
    }

    @JvmStatic
    fun customBlockMechanic(block: Block): CustomBlockMechanic? {
        return CustomBlockRegistry.getMechanic(block)
    }

    @JvmStatic
    fun customBlockMechanic(blockData: BlockData): CustomBlockMechanic? {
        return CustomBlockRegistry.getMechanic(blockData)
    }

    @JvmStatic
    fun customBlockMechanic(itemID: String) = CustomBlockRegistry.getMechanic(itemID)

    @JvmStatic
    fun noteBlockMechanic(data: BlockData?): NoteBlockMechanic? {
        return NoteBlockMechanicFactory.instance()?.getMechanic(data ?: return null)
    }

    @JvmStatic
    fun noteBlockMechanic(block: Block): NoteBlockMechanic? {
        return NoteBlockMechanicFactory.instance()?.getMechanic(block.blockData)
    }

    @JvmStatic
    fun noteBlockMechanic(itemID: String?) = NoteBlockMechanicFactory.instance()?.getMechanic(itemID)

    @JvmStatic
    fun stringMechanic(blockData: BlockData?): StringBlockMechanic? {
        return StringBlockMechanicFactory.instance()?.getMechanic(blockData ?: return null)
    }

    @JvmStatic
    fun stringMechanic(block: Block): StringBlockMechanic? {
        return StringBlockMechanicFactory.instance()?.getMechanic(block.blockData)
    }

    @JvmStatic
    fun stringMechanic(itemID: String?) =
        StringBlockMechanicFactory.instance()?.getMechanic(itemID)

    @JvmStatic
    fun chorusBlockMechanic(blockData: BlockData?): ChorusBlockMechanic? {
        return ChorusBlockFactory.instance()?.getMechanic(blockData ?: return null)
    }

    @JvmStatic
    fun chorusBlockMechanic(block: Block): ChorusBlockMechanic? {
        return ChorusBlockFactory.instance()?.getMechanic(block.blockData)
    }

    @JvmStatic
    fun chorusBlockMechanic(itemID: String?) = ChorusBlockFactory.instance()?.getMechanic(itemID)
}
