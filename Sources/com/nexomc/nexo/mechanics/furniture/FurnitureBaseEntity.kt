package com.nexomc.nexo.mechanics.furniture

import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.items.ItemBuilder
import com.nexomc.nexo.utils.ItemUtils
import com.nexomc.nexo.utils.VersionUtil
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.*

class FurnitureBaseEntity(baseEntity: ItemDisplay, val mechanic: FurnitureMechanic) {
    var itemStack: ItemStack = let {
        val itemBuilder = mechanic.placedItemModel?.takeIf { VersionUtil.atleast("1.20.5") }
            ?.let { ItemBuilder(Material.BARRIER).setItemModel(NamespacedKey.fromString(it.asString())) }
            ?: NexoItems.itemFromId(mechanic.placedItemId)
            ?: NexoItems.itemFromId(mechanic.itemID)
            ?: ItemBuilder(Material.BARRIER)

        itemBuilder.apply {
            customTag(NexoItems.ITEM_ID, PersistentDataType.STRING, mechanic.itemID)
        }.build().also {
            ItemUtils.dyeItem(it, FurnitureHelpers.furnitureDye(baseEntity))
            ItemUtils.displayName(it, null)
        }
    }
        set(value) {
            ItemUtils.dyeItem(value, FurnitureHelpers.furnitureDye(baseEntity()!!))
            ItemUtils.displayName(value, null)
        }
    val baseUuid: UUID = baseEntity.uniqueId
    val baseId: Int = baseEntity.entityId
    val furnitureId: String = mechanic.itemID

    fun baseEntity(): ItemDisplay? {
        return Bukkit.getEntity(baseUuid) as ItemDisplay?
    }

    fun mechanic(): FurnitureMechanic {
        return NexoFurniture.furnitureMechanic(furnitureId) ?: mechanic
    }

    override fun equals(other: Any?): Boolean {
        return other is FurnitureBaseEntity && this.baseUuid == other.baseUuid && this.baseId == other.baseId && mechanic.itemID == other.mechanic().itemID
    }
}
