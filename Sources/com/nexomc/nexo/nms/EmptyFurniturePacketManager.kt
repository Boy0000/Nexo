package com.nexomc.nexo.nms

import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager
import org.bukkit.entity.Entity
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player

class EmptyFurniturePacketManager : IFurniturePacketManager {
    override fun nextEntityId(): Int {
        return -1
    }

    override fun getEntity(entityId: Int): Entity? {
        return null
    }

    override fun sendInteractionEntityPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {
    }

    override fun sendInteractionEntityPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player) {
    }

    override fun removeInteractionHitboxPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {
    }

    override fun removeInteractionHitboxPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player) {
    }

    override fun sendShulkerEntityPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {
    }

    override fun sendShulkerEntityPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player) {
    }

    override fun removeShulkerHitboxPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {
    }

    override fun removeShulkerHitboxPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player) {
    }

    override fun sendBarrierHitboxPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {
    }

    override fun sendBarrierHitboxPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player) {
    }

    override fun removeBarrierHitboxPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {
    }

    override fun removeBarrierHitboxPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player) {
    }
}
