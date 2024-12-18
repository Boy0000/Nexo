package com.nexomc.nexo.mechanics.misc.commands

import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.utils.AdventureUtils.tagResolver
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

class CommandsMechanicListener(private val factory: CommandsMechanicFactory) : Listener {
    @EventHandler(priority = EventPriority.NORMAL)
    fun PlayerInteractEvent.onInteract() {
        val item = item ?: return
        val mechanic = factory.getMechanic(item)
        if (action == Action.PHYSICAL || mechanic == null) return

        if (!mechanic.hasPermission(player)) {
            Message.NO_PERMISSION.send(player, tagResolver("permission", mechanic.permission ?: ""))
            return
        }

        mechanic.getTimer(player).let { it.takeIf { it.isFinished }?.reset() ?: return it.sendToPlayer(player) }

        mechanic.commands.perform(player)

        if (mechanic.isOneUsage) item.amount -= 1
    }
}
