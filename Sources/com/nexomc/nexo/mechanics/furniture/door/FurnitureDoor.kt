package com.nexomc.nexo.mechanics.furniture.door

import com.nexomc.nexo.mechanics.furniture.FurnitureFactory
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.mechanics.furniture.FurnitureProperties
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager
import com.nexomc.nexo.mechanics.furniture.hitbox.FurnitureHitbox
import com.nexomc.nexo.mechanics.furniture.hitbox.InteractionHitbox
import com.nexomc.nexo.utils.clone
import com.nexomc.nexo.utils.copyFrom
import com.nexomc.nexo.utils.getStringOrNull
import com.nexomc.nexo.utils.mapFastSet
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.joml.Quaternionf

data class FurnitureDoor(
    private val openSound: String?,
    private val closeSound: String? = openSound,
    private val toggleHitboxOnOpen: Boolean = true,
    private val isSliding: Boolean = false,
    val openProperties: FurnitureProperties?
) {

    constructor(section: ConfigurationSection) : this(
        section.getStringOrNull("open_sound"),
        section.getStringOrNull("close_sound") ?: section.getStringOrNull("open_sound"),
        section.getBoolean("toggle_hitbox_on_open", true),
        section.getBoolean("is_sliding", false),

        (section.parent?.getConfigurationSection("properties")?.clone() ?: YamlConfiguration())
            .copyFrom(section.getConfigurationSection("open_properties")).let(::FurnitureProperties).apply {
                if (!section.getBoolean("is_sliding", false) && leftRotation == Quaternionf())
                    this.leftRotation.set(0f, 0.707f, 0f, 0.707f)
            }
    )

    fun toggleState(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {
        val isOpen = baseEntity.persistentDataContainer.get(DOOR_OPEN_STATE, PersistentDataType.BOOLEAN) ?: false
        baseEntity.persistentDataContainer.set(DOOR_OPEN_STATE, PersistentDataType.BOOLEAN, !isOpen)

        ensureHitboxState(baseEntity, mechanic)

        if (!isOpen) openSound?.let { baseEntity.world.playSound(baseEntity.location, it, 1f, 1f)}
        else closeSound?.let { baseEntity.world.playSound(baseEntity.location, it, 1f, 1f)}
    }

    fun ensureHitboxState(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player? = null) {
        val packetManager = FurnitureFactory.instance()?.packetManager() ?: return
        val isOpen = baseEntity.persistentDataContainer.get(DOOR_OPEN_STATE, PersistentDataType.BOOLEAN) ?: false

        mechanic.hitbox.refreshHitboxes(baseEntity, mechanic)

        if (isOpen) {
            val fakeMechanic = openHitboxMechanic(baseEntity, mechanic)
            IFurniturePacketManager.updateBaseEntity(baseEntity, fakeMechanic)
            if (player == null) {
                packetManager.sendFurnitureMetadataPacket(baseEntity, fakeMechanic)
                if (toggleHitboxOnOpen) packetManager.removeBarrierHitboxPacket(baseEntity, mechanic)
                if (toggleHitboxOnOpen) packetManager.sendHitboxEntityPacket(baseEntity, fakeMechanic)
            } else {
                packetManager.sendFurnitureMetadataPacket(baseEntity, fakeMechanic, player)
                if (toggleHitboxOnOpen) packetManager.removeBarrierHitboxPacket(baseEntity, mechanic, player)
                if (toggleHitboxOnOpen) packetManager.sendHitboxEntityPacket(baseEntity, fakeMechanic, player)
            }
        } else {
            packetManager.sendFurnitureMetadataPacket(baseEntity, mechanic)
        }
        IFurniturePacketManager.updateBaseEntity(baseEntity, mechanic)
    }

    /**
     * Returns a clone of the hitbox, rotating and swapping barriers for interaction-entities
     */
    private fun openHitboxMechanic(baseEntity: ItemDisplay, mechanic: FurnitureMechanic): FurnitureMechanic {
        val hitboxes = mechanic.hitbox.barriers.mapFastSet {
            val blockLoc = if (isSliding) it else it.groundRotate(baseEntity.yaw + 90).add(0,0,0)
            InteractionHitbox(blockLoc.toVector())
        }
        return mechanic.clone().apply {
            properties = openProperties ?: properties
            hitbox = FurnitureHitbox(interactions = hitboxes)
        }
    }

    companion object {
        val DOOR_OPEN_STATE = NamespacedKey.fromString("nexo:door_open_state")!!
    }

}