package com.nexomc.nexo.mechanics.custom_block.noteblock.logstrip

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.mechanics.misc.misc.MiscMechanicFactory
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

class LogStripListener : Listener {
    @EventHandler
    fun PlayerInteractEvent.onStrippingLog() {
        val block = clickedBlock ?: return
        val item = player.inventory.itemInMainHand

        if (action != Action.RIGHT_CLICK_BLOCK || block.type != Material.NOTE_BLOCK || !canStripLog(item)) return

        val mechanic = NexoBlocks.noteBlockMechanic(block) ?: return
        val log = mechanic.log()?.takeIf { it.canBeStripped() } ?: return
        if (log.hasStrippedDrop()) player.world.dropItemNaturally(block.getRelative(player.facing.getOppositeFace()).location.toCenterLocation(), log.logDrop)
        if (log.decreaseAxeDurability && player.gameMode != GameMode.CREATIVE) item.damage(1, player)

        // If the block being stripped is a directional non-parent
        // and the log-mechanic returned above links to a stripped directional parent
        // get the subblock the current block is, and the equivalent child of stripped-directional
        val strippedBlock = log.stripMechanic?.let {
            when {
                mechanic.directional?.isParentBlock() == false && it.directional?.isParentBlock() == true -> {
                    val parent = mechanic.directional.parentMechanic!!.directional!!
                    when (mechanic.itemID) {
                        parent.xBlock -> NexoBlocks.noteBlockMechanic(it.directional.xBlock) ?: it
                        parent.yBlock -> NexoBlocks.noteBlockMechanic(it.directional.yBlock) ?: it
                        parent.zBlock -> NexoBlocks.noteBlockMechanic(it.directional.zBlock) ?: it
                        else -> it
                    }
                }
                else -> it
            }
        }?.blockData ?: return

        block.blockData = strippedBlock
        player.playSound(block.location, Sound.ITEM_AXE_STRIP, 1.0f, 0.8f)
    }

    private fun canStripLog(itemStack: ItemStack): Boolean {
        return itemStack.type.name.endsWith("_AXE") || (MiscMechanicFactory.instance()?.getMechanic(NexoItems.idFromItem(itemStack))?.canStripLogs
            ?: false)
    }
}
