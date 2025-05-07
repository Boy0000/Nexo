package com.nexomc.nexo.mechanics.furniture

import com.jeff_media.morepersistentdatatypes.DataType
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager.Companion.furnitureBaseMap
import org.bukkit.Color
import org.bukkit.Rotation
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

typealias FurnitureTransform = ItemDisplay.ItemDisplayTransform

object FurnitureHelpers {
    fun correctedYaw(mechanic: FurnitureMechanic, yaw: Float): Float {
        return when {
            mechanic.limitedPlacing?.isRoof == false -> yaw
            mechanic.properties.isFixedTransform -> yaw - 180
            else -> yaw
        }
    }

    @JvmStatic
    fun furnitureYaw(baseEntity: ItemDisplay, yaw: Float) {
        if (!NexoFurniture.isFurniture(baseEntity)) return

        baseEntity.setRotation(yaw, baseEntity.pitch)
    }

    @JvmStatic
    fun rotationToYaw(rotation: Rotation) = (Rotation.entries.indexOf(rotation) * 360f) / 8f

    @JvmStatic
    fun furnitureItem(baseEntity: ItemDisplay): ItemStack? {
        return furnitureBaseMap.get(baseEntity.uniqueId)?.itemStack
    }

    @JvmStatic
    fun furnitureItem(baseEntity: ItemDisplay, itemStack: ItemStack) {
        NexoFurniture.furnitureMechanic(baseEntity) ?: return
        furnitureBaseMap.get(baseEntity.uniqueId)?.itemStack(itemStack, baseEntity)
    }

    @JvmStatic
    fun furnitureDye(baseEntity: ItemDisplay) =
        baseEntity.persistentDataContainer.get(FurnitureMechanic.FURNITURE_DYE_KEY, PersistentDataType.INTEGER)?.let(Color::fromRGB)

    @JvmStatic
    fun furnitureDye(baseEntity: ItemDisplay, dyeColor: Color?) {
        if (dyeColor == null) baseEntity.persistentDataContainer.remove(FurnitureMechanic.FURNITURE_DYE_KEY)
        else baseEntity.persistentDataContainer.set(FurnitureMechanic.FURNITURE_DYE_KEY, PersistentDataType.INTEGER, dyeColor.asRGB())
        furnitureBaseMap.get(baseEntity.uniqueId)?.refreshItem(baseEntity)
    }

    @JvmStatic
    fun lightState(baseEntity: ItemDisplay): Boolean {
        if (NexoFurniture.furnitureMechanic(baseEntity)?.light?.isEmpty != false) return false
        return baseEntity.persistentDataContainer.getOrDefault(FurnitureMechanic.FURNITURE_LIGHT_KEY, DataType.BOOLEAN, true)
    }

    @JvmOverloads
    @JvmStatic
    fun toggleLight(baseEntity: ItemDisplay, state: Boolean? = null, mechanic: FurnitureMechanic? = NexoFurniture.furnitureMechanic(baseEntity)): Boolean {
        val mechanic = mechanic?.takeUnless { it.light.isEmpty } ?: return false
        val newState = state ?: !baseEntity.persistentDataContainer.getOrDefault(FurnitureMechanic.FURNITURE_LIGHT_KEY, DataType.BOOLEAN, true)
        if (!mechanic.light.toggleable) return true

        baseEntity.persistentDataContainer.set(FurnitureMechanic.FURNITURE_LIGHT_KEY, DataType.BOOLEAN, state ?: newState)
        furnitureBaseMap.get(baseEntity.uniqueId)?.refreshItem(baseEntity)
        mechanic.light.refreshLights(baseEntity, mechanic)
        return newState
    }
}
