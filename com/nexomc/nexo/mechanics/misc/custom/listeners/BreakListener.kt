package com.nexomc.nexo.mechanics.misc.custom.listeners

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.mechanics.misc.custom.fields.CustomEvent
import com.nexomc.nexo.utils.actions.ClickAction
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerItemBreakEvent

class BreakListener(itemID: String?, cooldown: Long, event: CustomEvent, clickAction: ClickAction) :
    CustomListener(itemID, cooldown, event, clickAction) {
    @EventHandler
    fun PlayerItemBreakEvent.onBroken() {
        val item = brokenItem
        if (itemID != NexoItems.idFromItem(item)) return
        perform(player, item)
    }
}
