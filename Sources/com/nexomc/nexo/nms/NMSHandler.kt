package com.nexomc.nexo.nms

import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager
import com.nexomc.nexo.mechanics.furniture.bed.EmptyCustomEntityHandler
import com.nexomc.nexo.utils.InteractionResult
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

interface NMSHandler {

    val packDispatchListener: Listener

    val pluginConverter: IPluginConverter

    fun customEntityHandler(): ICustomEntityHandler = EmptyCustomEntityHandler()

    fun furniturePacketManager(): IFurniturePacketManager = EmptyFurniturePacketManager()

    fun packetHandler(): IPacketHandler

    fun itemUtils(): IItemUtils

    fun playerUtils(): IPlayerUtils

    fun noteblockUpdatesDisabled(): Boolean

    fun tripwireUpdatesDisabled(): Boolean

    fun chorusplantUpdateDisabled(): Boolean

    fun resourcepackFormat(): Int
    fun datapackFormat(): Int

    /**
     * Corrects the BlockData of a placed block.
     * Mainly fired when placing a block against an NexoNoteBlock due to vanilla behaviour requiring Sneaking
     *
     * @param player          The player that placed the block
     * @param slot            The hand the player placed the block with
     * @param itemStack       The ItemStack the player placed the block with
     * @param target          The block where the new block will be placed (usually air or another replaceable block)
     * @param blockFace       The face of the block that the player clicked
     * @return The enum interaction result
     */
    fun correctBlockStates(player: Player, slot: EquipmentSlot, itemStack: ItemStack, target: Block, blockFace: BlockFace): InteractionResult?

    fun noteBlockInstrument(block: Block): String
}
