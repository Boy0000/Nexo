package com.nexomc.nexo.mechanics.custom_block.noteblock.logstrip

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteBlockMechanicFactory
import com.nexomc.nexo.mechanics.misc.misc.MiscMechanicFactory
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

class LogStripListener : Listener {
    @EventHandler
    fun PlayerInteractEvent.onStrippingLog() {
        val block = clickedBlock ?: return
        val item = player.inventory.itemInMainHand

        if (action != Action.RIGHT_CLICK_BLOCK || block.type != Material.NOTE_BLOCK || !canStripLog(item)) return

        var mechanic = NexoBlocks.noteBlockMechanic(block) ?: return
        mechanic = mechanic.directional?.takeUnless { it.isParentBlock() && mechanic.isLog }?.parentMechanic ?: return

        val log = mechanic.log()
        if (!mechanic.isLog || !log!!.canBeStripped()) return

        if (log.hasStrippedDrop()) {
            player.world.dropItemNaturally(
                block.getRelative(player.facing.getOppositeFace()).location,
                NexoItems.itemFromId(log.strippedLogDrop)?.build() ?: ItemStack(Material.AIR)
            )
        }

        (item.itemMeta as? Damageable).takeIf { log.shouldDecreaseAxeDurability() && player.gameMode != GameMode.CREATIVE }?.let { axeMeta ->
            val maxDurability = item.type.maxDurability.toInt()

            when {
                axeMeta.damage + 1 <= maxDurability -> {
                    axeMeta.damage += 1
                    item.itemMeta = axeMeta
                }
                else -> {
                    player.playSound(player.location, Sound.ENTITY_ITEM_BREAK, 1f, 1f)
                    item.type = Material.AIR
                }
            }
        }

        NoteBlockMechanicFactory.setBlockModel(block, log.strippedLogDrop)
        player.playSound(block.location, Sound.ITEM_AXE_STRIP, 1.0f, 0.8f)
    }

    private fun canStripLog(itemStack: ItemStack): Boolean {
        return itemStack.type.name.endsWith("_AXE") || (MiscMechanicFactory.instance()?.getMechanic(NexoItems.idFromItem(itemStack))?.canStripLogs() ?: false)
    }
}
