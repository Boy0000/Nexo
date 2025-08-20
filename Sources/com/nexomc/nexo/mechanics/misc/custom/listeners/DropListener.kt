package com.nexomc.nexo.mechanics.misc.custom.listeners

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.mechanics.misc.custom.fields.CustomEvent
import com.nexomc.nexo.utils.actions.ClickAction
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerDropItemEvent

class DropListener(itemID: String?, cooldown: Long, event: CustomEvent, clickAction: ClickAction) :
    CustomListener(itemID, cooldown, event, clickAction) {
    @EventHandler
    fun PlayerDropItemEvent.onDropped() {
        val item = itemDrop.itemStack
        if (itemID != NexoItems.idFromItem(item)) return
        if (!perform(player, item)) return
        if (event.cancelEvent) isCancelled = true
    }
}
