package com.nexomc.nexo.api.events.custom_block.chorusblock

import com.nexomc.nexo.api.events.custom_block.NexoBlockInteractEvent
import com.nexomc.nexo.mechanics.custom_block.chorusblock.ChorusBlockMechanic
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

class NexoChorusBlockInteractEvent(
    override val mechanic: ChorusBlockMechanic,
    player: Player,
    itemInHand: ItemStack?,
    hand: EquipmentSlot,
    block: Block,
    blockFace: BlockFace,
    action: Action
) : NexoBlockInteractEvent(mechanic, player, itemInHand, hand, block, blockFace, action)
