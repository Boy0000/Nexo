package com.nexomc.nexo.api.events.custom_block

import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic
import com.nexomc.nexo.utils.drops.DroppedLoot
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

open class NexoCustomBlockDropLootEvent(
    open val mechanic: CustomBlockMechanic,
    val block: Block,
    val player: Player,
    val loots: List<DroppedLoot>
) : Event() {

    override fun getHandlers() = handlerList

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
