package com.nexomc.nexo.mechanics.furniture.bed

import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerAnimationEvent
import org.bukkit.event.player.PlayerAnimationType
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.world.TimeSkipEvent

class FurnitureBedListener(val bedPlayer: Player, val bed: FurnitureBed) : Listener {

    @EventHandler
    fun TimeSkipEvent.onSleep() {
        if (skipReason == TimeSkipEvent.SkipReason.NIGHT_SKIP && bed.skipNight) bedPlayer.leaveVehicle()
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun PlayerInteractEvent.interact() {
        if (player == bedPlayer) isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun PlayerInteractEntityEvent.onInteractEntity() {
        if (player === bedPlayer) isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun EntityDamageByEntityEvent.onDamageEntity() {
        if (damager === bedPlayer) isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun EntityDamageEvent.onEntityDamage() {
        if (entity === bedPlayer) bedPlayer.swingMainHand()
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun PlayerAnimationEvent.onAnimation() {
        if (player === bedPlayer && animationType == PlayerAnimationType.ARM_SWING) bedPlayer.swingMainHand()
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun ProjectileLaunchEvent.onProjectile() {
        if (entity.shooter === bedPlayer) isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun InventoryClickEvent.onClick() {
        if (whoClicked === bedPlayer && bedPlayer.gameMode == GameMode.CREATIVE) isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun PlayerDropItemEvent.dropItem() {
        if (player === bedPlayer && bedPlayer.gameMode == GameMode.CREATIVE) isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun EntityPotionEffectEvent.onEffect() {
        if (entity === bedPlayer) bedPlayer.isInvisible = true
    }
}