package com.nexomc.nexo.mechanics.misc.custom.listeners

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.mechanics.misc.custom.fields.CustomEvent
import com.nexomc.nexo.utils.actions.ClickAction
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.PlayerDeathEvent

class DeathListener(itemID: String?, cooldown: Long, event: CustomEvent, clickAction: ClickAction) :
    CustomListener(itemID, cooldown, event, clickAction) {
    @EventHandler
    fun PlayerDeathEvent.onDeath() {
        drops.asSequence().forEach { if (itemID == NexoItems.idFromItem(it)) perform(entity.player!!, it!!) }
    }
}
