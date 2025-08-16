package com.nexomc.nexo.api.events.custom_block.stringblock

import com.nexomc.nexo.api.events.custom_block.NexoBlockInteractEvent
import com.nexomc.nexo.mechanics.custom_block.stringblock.StringBlockMechanic
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

class NexoStringBlockInteractEvent(
    override val mechanic: StringBlockMechanic,
    player: Player,
    itemInHand: ItemStack?,
    hand: EquipmentSlot,
    block: Block,
    blockFace: BlockFace,
    action: Action
) : NexoBlockInteractEvent(mechanic, player, itemInHand, hand, block, blockFace, action)