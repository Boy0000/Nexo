package com.nexomc.nexo.mechanics.misc.custom.listeners

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.mechanics.misc.custom.fields.CustomEvent
import com.nexomc.nexo.utils.actions.ClickAction
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.inventory.PlayerInventory

class DropAllListener(itemID: String?, cooldown: Long, event: CustomEvent, clickAction: ClickAction) :
    CustomListener(itemID, cooldown, event, clickAction) {
    @EventHandler
    fun PlayerDropItemEvent.onDropped() {
        val item = itemDrop.itemStack
        if (itemID != NexoItems.idFromItem(item)) return;
        if (itemID != null && player.inventory.containsItemWithId(itemID)) return
        perform(player, item)
    }

    private fun PlayerInventory.containsItemWithId(itemID: String): Boolean {
        return contents.any { it != null && NexoItems.idFromItem(it) == itemID }
    }
}
