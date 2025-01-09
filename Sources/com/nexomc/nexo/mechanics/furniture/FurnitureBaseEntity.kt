package com.nexomc.nexo.mechanics.furniture

import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.utils.ItemUtils
import org.bukkit.Bukkit
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.*

class FurnitureBaseEntity(baseEntity: ItemDisplay, val mechanic: FurnitureMechanic) {
    fun refreshItem(baseEntity: ItemDisplay) {
        itemStack = (mechanic.light.lightModelItem(baseEntity) ?: mechanic.placedItem).apply {
            customTag(NexoItems.ITEM_ID, PersistentDataType.STRING, mechanic.itemID)
        }.build().also {
            ItemUtils.dyeItem(it, FurnitureHelpers.furnitureDye(baseEntity))
            ItemUtils.displayName(it, null)
        }
        FurnitureFactory.instance()?.packetManager()?.sendFurnitureMetadataPacket(baseEntity, mechanic)
    }
    fun itemStack(item: ItemStack, baseEntity: ItemDisplay) {
        ItemUtils.dyeItem(item, FurnitureHelpers.furnitureDye(baseEntity))
        ItemUtils.displayName(item, null)
        ItemUtils.editItemMeta(item) {
            it.persistentDataContainer.set(NexoItems.ITEM_ID, PersistentDataType.STRING, mechanic.itemID)
        }
        itemStack = item
        FurnitureFactory.instance()?.packetManager()?.sendFurnitureMetadataPacket(baseEntity, mechanic)
    }
    var itemStack: ItemStack = (mechanic.light.lightModelItem(baseEntity) ?: mechanic.placedItem).apply {
        customTag(NexoItems.ITEM_ID, PersistentDataType.STRING, mechanic.itemID)
    }.build().also {
        ItemUtils.dyeItem(it, FurnitureHelpers.furnitureDye(baseEntity))
        ItemUtils.displayName(it, null)
    }
        private set
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
