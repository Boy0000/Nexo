package com.nexomc.nexo.compatibilities.worldguard

import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent
import com.nexomc.nexo.compatibilities.CompatibilityProvider
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.bukkit.WorldGuardPlugin
import com.sk89q.worldguard.protection.flags.StateFlag
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

class WorldGuardCompatibility : CompatibilityProvider<WorldGuardPlugin>() {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun NexoFurnitureInteractEvent.onInteract() {
        val localPlayer = plugin?.wrapPlayer(player) ?: return
        val container = WorldGuard.getInstance().platform.regionContainer
        val query = container.createQuery()

        val location = BukkitAdapter.adapt(baseEntity.location)
        val state = query.queryState(location, localPlayer, NexoWorldguardFlags.FURNITURE_INTERACT_FLAG) ?: return

        useFurniture = if (StateFlag.test(state)) Event.Result.ALLOW else Event.Result.DENY
    }
}