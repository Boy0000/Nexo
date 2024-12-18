package com.nexomc.nexo.utils.breaker

import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import org.bukkit.block.Block
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player

interface BreakerManager {
    fun startFurnitureBreak(player: Player, baseEntity: ItemDisplay, mechanic: FurnitureMechanic, block: Block)
    fun startBlockBreak(player: Player, block: Block, mechanic: CustomBlockMechanic)
    fun stopBlockBreak(player: Player)
}
