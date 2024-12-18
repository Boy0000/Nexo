package com.nexomc.nexo.mechanics.misc.custom.listeners

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.mechanics.misc.custom.fields.CustomEvent
import com.nexomc.nexo.utils.actions.ClickAction
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryClickEvent

class InvClickListener(itemID: String?, cooldown: Long, event: CustomEvent, clickAction: ClickAction) :
    CustomListener(itemID, cooldown, event, clickAction) {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun InventoryClickEvent.onInvClick() {
        val clicked = currentItem
        if (clicked != null && itemID == NexoItems.idFromItem(clicked)) perform(whoClicked as Player, clicked)
    }
}
