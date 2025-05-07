package com.nexomc.nexo.mechanics.trident

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import com.nexomc.nexo.nms.NMSHandlers
import com.nexomc.nexo.utils.SchedulerUtils
import io.papermc.paper.event.player.PlayerTrackEntityEvent
import org.bukkit.entity.Trident
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class CustomTridentListener : Listener {

    @EventHandler
    fun EntityAddToWorldEvent.onAdd() {
        val trident = entity as? Trident ?: return
        val mechanic = TridentFactory.instance()?.getMechanic(trident.itemStack) ?: return

        val tridentEntity = NMSHandlers.handler().customEntityHandler().createTridentEntity(trident, mechanic) ?: return
        SchedulerUtils.syncDelayedTask(1L) {
            tridentEntity.spawn(trident.location)
        }
    }

    @EventHandler
    fun PlayerTrackEntityEvent.onTrack() {
         if (TridentFactory.instance()?.getMechanic((entity as? Trident)?.itemStack) != null) isCancelled = true
    }
}