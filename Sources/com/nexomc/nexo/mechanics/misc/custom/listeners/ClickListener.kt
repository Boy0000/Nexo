package com.nexomc.nexo.mechanics.misc.custom.listeners

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.mechanics.misc.custom.fields.CustomEvent
import com.nexomc.nexo.utils.actions.ClickAction
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

class ClickListener(itemID: String?, cooldown: Long, event: CustomEvent, clickAction: ClickAction)
    : CustomListener(itemID, cooldown, event, clickAction) {
    private val interactActions = mutableSetOf<Action>()

    init {
        when (event.getParams()[0]) {
            "right" -> when {
                event.getParams()[1] == "all" -> {
                    interactActions += Action.RIGHT_CLICK_AIR
                    interactActions += Action.RIGHT_CLICK_BLOCK
                }
                event.getParams()[1] == "block" -> interactActions += Action.RIGHT_CLICK_BLOCK
                else -> interactActions += Action.RIGHT_CLICK_AIR
            }

            "left" -> when {
                event.getParams()[1] == "all" -> {
                    interactActions += Action.LEFT_CLICK_AIR
                    interactActions += Action.LEFT_CLICK_BLOCK
                }
                event.getParams()[1] == "block" -> interactActions.add(Action.LEFT_CLICK_BLOCK)
                else -> interactActions += Action.LEFT_CLICK_AIR
            }

            "all" -> when {
                event.getParams()[1] == "all" -> {
                    interactActions += Action.RIGHT_CLICK_AIR
                    interactActions += Action.RIGHT_CLICK_BLOCK
                    interactActions += Action.LEFT_CLICK_AIR
                    interactActions += Action.LEFT_CLICK_BLOCK
                }
                event.getParams()[1] == "block" -> {
                    interactActions += Action.RIGHT_CLICK_BLOCK
                    interactActions += Action.LEFT_CLICK_BLOCK
                }
                else -> {
                    interactActions += Action.RIGHT_CLICK_AIR
                    interactActions += Action.LEFT_CLICK_AIR
                }
            }

            else -> throw IllegalStateException("Unexpected value: ${event.getParams()[0]}")
        }
    }

    @EventHandler
    fun PlayerInteractEvent.onClicked() {
        if (!interactActions.contains(action)) return
        if (itemID != NexoItems.idFromItem(item)) return
        if (!perform(player, item!!)) return
        if (event.cancelEvent) isCancelled = true
    }
}
