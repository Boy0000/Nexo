package com.nexomc.nexo.utils.breaker

import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic
import org.bukkit.block.Block
import org.bukkit.entity.Player

interface BreakerManager {
    fun startBlockBreak(player: Player, block: Block, mechanic: CustomBlockMechanic)
    fun stopBlockBreak(player: Player)
}
