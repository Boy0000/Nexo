package com.nexomc.nexo.utils

import org.bukkit.Bukkit
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.entity.Entity
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent

object EventUtils {
    /**
     * Calls the event and tests if cancelled.
     *
     * @return false if event was cancelled, if cancellable. otherwise true.
     */
    fun Event.call(): Boolean {
        Bukkit.getPluginManager().callEvent(this)
        return when (this) {
            is Cancellable -> !this.isCancelled
            else -> true
        }
    }

    fun <T : Event> T.call(block: T.() -> Unit) {
        Bukkit.getPluginManager().callEvent(this)
        if (this is Cancellable && this.isCancelled.not()) block()
    }

    /** In a recent build of Spigot 1.20.4, they removed undeprecated constructors for EntityDamageByEntityEvent here. This method aims to call the event with backwards compatibility  */
    @JvmStatic
    fun EntityDamageByEntityEvent(
        damager: Entity,
        entity: Entity,
        cause: EntityDamageEvent.DamageCause,
        damageType: DamageType,
        damage: Double
    ): EntityDamageByEntityEvent {
        return runCatching {
            // Old constructor
            EntityDamageByEntityEvent::class.java.getConstructor(
                Entity::class.java,
                Entity::class.java,
                EntityDamageEvent.DamageCause::class.java,
                Double::class.javaPrimitiveType
            ).newInstance(damager, entity, cause, damage)
        }.getOrDefault(EntityDamageByEntityEvent(damager, entity, cause, DamageSource.builder(damageType).build(), damage))
    }
}
