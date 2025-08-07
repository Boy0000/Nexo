package com.nexomc.nexo.mechanics.misc.custom.listeners

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.mechanics.misc.custom.fields.CustomEvent
import com.nexomc.nexo.utils.actions.ClickAction
import org.bukkit.event.EventHandler

class EquipListener(itemID: String?, cooldown: Long, event: CustomEvent, clickAction: ClickAction) : CustomListener(itemID, cooldown, event, clickAction) {

    @EventHandler
    fun PlayerArmorChangeEvent.onEquipArmor() {
        if (itemID != NexoItems.idFromItem(newItem)) return
        perform(player, newItem)
    }
}
