package com.nexomc.nexo.mechanics.misc.custom.listeners

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.mechanics.misc.custom.fields.CustomEvent
import com.nexomc.nexo.utils.actions.ClickAction
import com.nexomc.nexo.utils.timers.TimersFactory
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack

abstract class CustomListener protected constructor(
    protected val itemID: String?, cooldown: Long,
    protected val event: CustomEvent,
    protected val clickAction: ClickAction
) : Listener {
    private val timers = TimersFactory(cooldown * 50)

    open fun register() {
        Bukkit.getPluginManager().registerEvents(this, NexoPlugin.instance())
    }

    fun unregister() {
        HandlerList.unregisterAll(this)
    }

    fun perform(player: Player, itemStack: ItemStack) {
        if (!clickAction.canRun(player)) return

        timers.getTimer(player).let { it.takeIf { it.isFinished }?.reset() ?: return it.sendToPlayer(player) }

        clickAction.performActions(player)

        if (event.isOneUsage) itemStack.amount -= 1
    }
}
