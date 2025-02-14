package com.nexomc.nexo.mechanics.combat.spell.witherskull

import com.nexomc.nexo.utils.BlockHelpers
import io.th0rgal.protectionlib.ProtectionLib
import org.bukkit.entity.WitherSkull
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

class WitherSkullMechanicListener(private val factory: WitherSkullMechanicFactory) : Listener {
    @EventHandler(priority = EventPriority.NORMAL)
    fun PlayerInteractEvent.onCall() {
        val mechanic = factory.getMechanic(item) ?: return
        val location = clickedBlock?.location ?: player.location

        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return
        if (useItemInHand() == Event.Result.DENY || !ProtectionLib.canUse(player, location)) return
        if (BlockHelpers.isInteractable(clickedBlock) && useInteractedBlock() == Event.Result.ALLOW) return

        mechanic.timer(player).let { it.takeIf { it.isFinished }?.reset() ?: return it.sendToPlayer(player) }

        mechanic.removeCharge(item!!)
        val spawningLocation = player.location.add(0.0, 1.0, 0.0)
        val direction = player.location.getDirection()
        spawningLocation.add(direction.normalize().multiply(2))
        val skull = player.world.spawn(spawningLocation, WitherSkull::class.java)
        skull.direction = direction
        skull.isCharged = mechanic.charged
    }
}
