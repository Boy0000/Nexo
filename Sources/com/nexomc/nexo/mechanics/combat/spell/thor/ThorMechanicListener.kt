package com.nexomc.nexo.mechanics.combat.spell.thor

import com.nexomc.nexo.utils.BlockHelpers
import com.nexomc.nexo.utils.ItemUtils
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.protectionlib.ProtectionLib
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

class ThorMechanicListener(private val factory: ThorMechanicFactory) : Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    fun PlayerInteractEvent.onCall() {
        val item = item?.takeUnless { VersionUtil.atleast("1.21.2") && player.hasCooldown(it) } ?: return
        val mechanic = factory.getMechanic(item) ?: return
        val targetBlock = runCatching {
            player.getTargetBlock(null, 50).location
        }.getOrNull() ?: return

        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return
        if (useItemInHand() == Event.Result.DENY || !ProtectionLib.canUse(player, targetBlock)) return
        if (BlockHelpers.isInteractable(clickedBlock) && useInteractedBlock() == Event.Result.ALLOW) return

        mechanic.timer(player).let { it.takeIf { it.isFinished }?.reset() ?: return it.sendToPlayer(player) }

        mechanic.removeCharge(item)
        (0..mechanic.lightningBoltsAmount).forEach { i ->
            player.world.strikeLightning(mechanic.randomizedLocation(targetBlock))
        }
        ItemUtils.triggerCooldown(player, item)
    }
}
