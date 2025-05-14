package com.nexomc.nexo.mechanics.furniture

import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.utils.asColorable
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.DyedItemColor
import org.bukkit.Bukkit
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.*

class FurnitureBaseEntity(baseEntity: ItemDisplay, private val mechanic: FurnitureMechanic) {
    fun refreshItem(baseEntity: ItemDisplay) {
        itemStack = (mechanic.placedItem(baseEntity)).apply {
            customTag(NexoItems.ITEM_ID, PersistentDataType.STRING, mechanic.itemID)
        }.build().also { item ->
            val color = FurnitureHelpers.furnitureDye(baseEntity)
            runCatching {
                //TODO Swap to NMS method when Colorable rework is merged
                item.unsetData(DataComponentTypes.CUSTOM_NAME)
                if (color != null) item.setData(DataComponentTypes.DYED_COLOR, DyedItemColor.dyedItemColor(color))
                else item.unsetData(DataComponentTypes.DYED_COLOR)
            }.onFailure {
                item.editMeta {
                    it.asColorable()?.color = color
                    it.displayName(null)
                }
            }
        }
        FurnitureFactory.instance()?.packetManager()?.sendFurnitureMetadataPacket(baseEntity, mechanic)
    }
    fun itemStack(item: ItemStack, baseEntity: ItemDisplay) {
        val color = FurnitureHelpers.furnitureDye(baseEntity)
        runCatching {
            //TODO Swap to NMS method when Colorable rework is merged
            if (color != null) item.setData(DataComponentTypes.DYED_COLOR, DyedItemColor.dyedItemColor(color))
            else item.resetData(DataComponentTypes.DYED_COLOR)
            item.unsetData(DataComponentTypes.CUSTOM_NAME)
            item.editPersistentDataContainer {
                it.set(NexoItems.ITEM_ID, PersistentDataType.STRING, mechanic.itemID)
            }
        }.onFailure {
            item.editMeta {
                it.asColorable()?.color = color
                it.displayName(null)
                it.persistentDataContainer.set(NexoItems.ITEM_ID, PersistentDataType.STRING, mechanic.itemID)
            }
        }
        itemStack = item
        FurnitureFactory.instance()?.packetManager()?.sendFurnitureMetadataPacket(baseEntity, mechanic)
    }
    var itemStack: ItemStack
        private set

    init {
        itemStack = (mechanic.placedItem(baseEntity)).apply {
            customTag(NexoItems.ITEM_ID, PersistentDataType.STRING, mechanic.itemID)
        }.build()

        val color = FurnitureHelpers.furnitureDye(baseEntity)
        runCatching {
            //TODO Swap to NMS method when Colorable rework is merged
            if (color != null) itemStack.setData(DataComponentTypes.DYED_COLOR, DyedItemColor.dyedItemColor(color))
            else itemStack.resetData(DataComponentTypes.DYED_COLOR)
            itemStack.unsetData(DataComponentTypes.CUSTOM_NAME)
        }.onFailure {
            itemStack.editMeta {
                it.asColorable()?.color = color
                it.displayName(null)
            }
        }
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
        return other is FurnitureBaseEntity &&
                this.baseUuid == other.baseUuid &&
                this.baseId == other.baseId &&
                mechanic.itemID == other.mechanic().itemID
    }

    override fun hashCode(): Int {
        var result = baseId
        result = 31 * result + mechanic.hashCode()
        result = 31 * result + itemStack.hashCode()
        result = 31 * result + baseUuid.hashCode()
        result = 31 * result + furnitureId.hashCode()
        return result
    }
}
