package com.nexomc.nexo.mechanics.furniture.compatibility

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoFurniture
import com.moulberry.axiom.event.AxiomManipulateEntityEvent
import com.nexomc.nexo.mechanics.furniture.FurnitureFactory
import com.nexomc.nexo.utils.logs.Logs
import org.bukkit.Bukkit
import org.bukkit.entity.ItemDisplay
import org.bukkit.event.Listener

class AxiomCompatibility : Listener {
    init {
        Logs.logInfo("Registering Axiom-Compatibility for furniture...")
    }

    @org.bukkit.event.EventHandler
    fun AxiomManipulateEntityEvent.onAxiomManipFurniture() {
        val baseEntity = entity as? ItemDisplay ?: return
        val mechanic = NexoFurniture.furnitureMechanic(baseEntity) ?: return
        val packetManager = FurnitureFactory.instance()?.packetManager() ?: return
        val r = FurnitureFactory.instance()!!.simulationRadius

        packetManager.removeFurnitureEntityPacket(baseEntity, mechanic)
        packetManager.removeInteractionHitboxPacket(baseEntity, mechanic)
        packetManager.removeBarrierHitboxPacket(baseEntity, mechanic)

        Bukkit.getScheduler().runTaskLater(
            NexoPlugin.instance(), Runnable {
            baseEntity.world.getNearbyPlayers(baseEntity.location, r).forEach { player ->
                packetManager.sendFurnitureEntityPacket(baseEntity, mechanic, player)
                    packetManager.sendInteractionEntityPacket(baseEntity, mechanic, player)
                    packetManager.sendBarrierHitboxPacket(baseEntity, mechanic, player)
                    packetManager.sendLightMechanicPacket(baseEntity, mechanic, player)
                }
            }, 2L
        )
    }
}
