package com.nexomc.nexo.mechanics.custom_block

import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.utils.drops.Drop
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

interface CustomBlockType<T : CustomBlockMechanic> {
    fun name(): String
    fun factory(): MechanicFactory?
    fun getMechanic(block: Block): T?
    fun getMechanic(blockData: BlockData): T?
    fun toolTypes(): List<String>

    fun placeCustomBlock(player: Player, hand: EquipmentSlot, item: ItemStack, mechanic: T, placedAgainst: Block, blockFace: BlockFace)
    fun placeCustomBlock(location: Location, itemID: String?)
    fun removeCustomBlock(block: Block, player: Player?, overrideDrop: Drop?): Boolean

    fun placeWorldEdit(location: Location, mechanic: T)
    fun removeWorldEdit(location: Location, mechanic: T)

    val clazz: Class<T>
}
