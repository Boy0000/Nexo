package com.nexomc.nexo.mechanics.combat.spell.fireball

import com.nexomc.nexo.utils.BlockHelpers
import com.nexomc.nexo.utils.ItemUtils
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.protectionlib.ProtectionLib
import org.bukkit.entity.Fireball
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

class FireballMechanicListener(private val factory: FireballMechanicFactory) : Listener {

    @EventHandler
    fun PlayerInteractEvent.onPlayerUse() {
        val item = item?.takeUnless { VersionUtil.atleast("1.21.2") && player.hasCooldown(it) } ?: return
        val mechanic = factory.getMechanic(item) ?: return
        val location = clickedBlock?.location ?: player.location

        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return
        if (useItemInHand() == Event.Result.DENY || !ProtectionLib.canUse(player, location)) return
        if (BlockHelpers.isInteractable(clickedBlock) && useInteractedBlock() == Event.Result.ALLOW) return

        mechanic.timer(player).let { it.takeIf { it.isFinished }?.reset() ?: return it.sendToPlayer(player) }

        val fireball = player.launchProjectile(Fireball::class.java)
        fireball.yield = mechanic.yield.toFloat()
        fireball.direction = fireball.direction.multiply(mechanic.speed)

        mechanic.removeCharge(item)
        ItemUtils.triggerCooldown(player, item)
    }
}
