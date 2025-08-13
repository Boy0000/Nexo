package com.nexomc.nexo.mechanics.furniture.states

import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class FurnitureStateListener : Listener {

    @EventHandler
    fun NexoFurnitureInteractEvent.onInteract() {
        val states = mechanic.states ?: return
        if (states.creativeModeOnly && player.gameMode != GameMode.CREATIVE) return

        FurnitureStates.cycleState(baseEntity, mechanic)
    }
}