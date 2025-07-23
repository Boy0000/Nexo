package com.nexomc.nexo.compatibilities.worldguard

import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent
import com.nexomc.nexo.compatibilities.CompatibilityProvider
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.bukkit.WorldGuardPlugin
import com.sk89q.worldguard.protection.flags.Flags
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

        // Query base interaction flag
        val interactState = query.queryState(location, localPlayer, NexoWorldguardFlags.FURNITURE_INTERACT_FLAG, Flags.INTERACT)

        // Query subflags independently (does not affect useFurniture)
        query.queryState(location, localPlayer, NexoWorldguardFlags.FURNITURE_TOGGLE_LIGHT_FLAG)?.toResult(canToggleLight)?.let { canToggleLight = it }
        query.queryState(location, localPlayer, NexoWorldguardFlags.FURNITURE_SIT_FLAG)?.toResult()?.let { canSit = it }
        query.queryState(location, localPlayer, NexoWorldguardFlags.FURNITURE_OPEN_STORAGE_FLAG)?.toResult(canOpenStorage)?.let { canOpenStorage = it }
        query.queryState(location, localPlayer, NexoWorldguardFlags.FURNITURE_CLICKACTION_FLAG)?.toResult(canRunAction)?.let { canRunAction = it }
        query.queryState(location, localPlayer, NexoWorldguardFlags.FURNITURE_ROTATE_FLAG)?.toResult(canRotate)?.let {
            if (it == Event.Result.ALLOW && mechanic.rotatable.shouldRotate(player)) canRotate = it
        }

        // Set useFurniture **only based on interactState**
        useFurniture = interactState.toResult(useFurniture)
    }

    private fun StateFlag.State?.toResult(default: Event.Result = Event.Result.DEFAULT): Event.Result {
        return when (this) {
            StateFlag.State.ALLOW -> Event.Result.ALLOW
            StateFlag.State.DENY -> Event.Result.DENY
            else -> default
        }
    }
}