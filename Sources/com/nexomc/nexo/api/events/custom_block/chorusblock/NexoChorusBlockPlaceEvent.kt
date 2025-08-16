package com.nexomc.nexo.api.events.custom_block.chorusblock

import com.nexomc.nexo.api.events.custom_block.NexoBlockPlaceEvent
import com.nexomc.nexo.mechanics.custom_block.chorusblock.ChorusBlockMechanic
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

class NexoChorusBlockPlaceEvent(
    override val mechanic: ChorusBlockMechanic,
    block: Block,
    player: Player,
    itemInHand: ItemStack,
    hand: EquipmentSlot
) : NexoBlockPlaceEvent(mechanic, block, player, itemInHand, hand)
