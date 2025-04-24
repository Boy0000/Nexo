package com.nexomc.nexo.mechanics.custom_block.stringblock

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.api.events.custom_block.stringblock.NexoStringBlockPlaceEvent
import com.nexomc.nexo.mechanics.custom_block.CustomBlockHelpers
import org.bukkit.FluidCollisionMode
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.player.PlayerInteractEvent

class StringBlockMechanicListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    fun PlayerInteractEvent.onPlacingString() {
        if (item?.type != Material.STRING || StringBlockMechanicFactory.instance()?.disableVanillaString == false) return

        setUseItemInHand(Event.Result.DENY)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun PlayerInteractEvent.onPlaceableOnWater() {
        val (item, hand) = (item?.takeUnless { it.isEmpty } ?: return) to (hand ?: return)
        val itemID = NexoItems.idFromItem(item) ?: return
        val placedAgainst = player.rayTraceBlocks(5.0, FluidCollisionMode.SOURCE_ONLY)?.hitBlock ?: return
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return

        val target = placedAgainst.getRelative(BlockFace.UP).takeIf { it.type == Material.AIR } ?: return
        val mechanic = (NexoBlocks.customBlockMechanic(itemID) as? StringBlockMechanic)?.takeIf { it.isPlaceableOnWater }?.let {
            NexoBlocks.stringMechanic(it.randomPlace().randomOrNull()) ?: it
        } ?: return

        CustomBlockHelpers.makePlayerPlaceBlock(player, hand, item, target, blockFace, mechanic, mechanic.blockData)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun BlockBreakEvent.onBreakingTall() {
        val blockBelow = block.getRelative(BlockFace.DOWN)
        val mechanic = NexoBlocks.stringMechanic(block)?.takeIf { it.isTall } ?: return
        if (NexoBlocks.stringMechanic(blockBelow) != mechanic) return
        isDropItems = false
        NexoBlocks.remove(blockBelow.location, player)
    }

    @EventHandler
    fun NexoStringBlockPlaceEvent.onPlaceStringOnString() {
        val below = block.getRelative(BlockFace.DOWN)
        if (NexoBlocks.isNexoStringBlock(below)) isCancelled = true
        if (below.type == Material.TRIPWIRE && NexoBlocks.isNexoStringBlock(below.getRelative(BlockFace.DOWN)))
            isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun BlockFromToEvent.onWaterUpdate() {
        val changed = toBlock
        val changedBelow = changed.getRelative(BlockFace.DOWN)
        if (!block.isLiquid || changed.type != Material.TRIPWIRE) return

        isCancelled = true
        val mechanicBelow = NexoBlocks.stringMechanic(changedBelow)
        if (NexoBlocks.isNexoStringBlock(changed)) NexoBlocks.remove(changed.location, null, true)
        else if (mechanicBelow != null && mechanicBelow.isTall)
            NexoBlocks.remove(changedBelow.location, forceDrop = true)
    }
}
