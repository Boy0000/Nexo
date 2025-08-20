package com.nexomc.nexo.mechanics.misc.custom.listeners

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.mechanics.misc.custom.fields.CustomEvent
import com.nexomc.nexo.utils.actions.ClickAction
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityPickupItemEvent

class PickupListener(itemID: String?, cooldown: Long, event: CustomEvent, clickAction: ClickAction) :
    CustomListener(itemID, cooldown, event, clickAction) {
    @EventHandler
    fun EntityPickupItemEvent.onPickedUp() {
        val player = entity as? Player ?: return
        val item = item.itemStack
        if (itemID != NexoItems.idFromItem(item)) return
        if (!perform(player, item)) return
        if (event.cancelEvent) isCancelled = true
    }
}
