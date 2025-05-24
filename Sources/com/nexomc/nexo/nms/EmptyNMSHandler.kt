package com.nexomc.nexo.nms

import com.nexomc.nexo.nms.IPacketHandler.EmptyPacketHandler
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

class EmptyNMSHandler : NMSHandler {
    override fun packetHandler(): IPacketHandler = EmptyPacketHandler()
    override fun itemUtils(): IItemUtils = IItemUtils.EmptyItemUtils()
    override fun playerUtils(): IPlayerUtils = IPlayerUtils.EmptyPlayerUtils()
    override val pluginConverter: IPluginConverter = IPluginConverter.EmptyPluginConverter()
    override val packDispatchListener: Listener = object : Listener {}

    override fun noteblockUpdatesDisabled(): Boolean = false
    override fun tripwireUpdatesDisabled(): Boolean = false
    override fun chorusplantUpdateDisabled(): Boolean = false

    override fun correctBlockStates(player: Player, slot: EquipmentSlot, itemStack: ItemStack, target: Block, blockFace: BlockFace) = null

    override fun noteBlockInstrument(block: Block): String {
        return "block.note_block.harp"
    }

    override fun resourcepackFormat() = 55
    override fun datapackFormat() = 71
}
