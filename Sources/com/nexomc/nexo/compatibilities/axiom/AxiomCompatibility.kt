package com.nexomc.nexo.compatibilities.axiom

import com.moulberry.axiom.AxiomPaper
import com.moulberry.axiom.event.AxiomAfterManipulateEntityEvent
import com.moulberry.axiom.event.AxiomSpawnEntityEvent
import com.moulberry.axiom.paperapi.AxiomCustomBlocksAPI
import com.moulberry.axiom.paperapi.AxiomCustomDisplayAPI
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.api.events.NexoItemsLoadedEvent
import com.nexomc.nexo.compatibilities.CompatibilityProvider
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteBlockMechanic
import com.nexomc.nexo.mechanics.custom_block.noteblock.directional.DirectionalBlock
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.mechanics.furniture.bed.FurnitureBed
import com.nexomc.nexo.mechanics.furniture.seats.FurnitureSeat
import com.nexomc.nexo.utils.AdventureUtils
import com.nexomc.nexo.utils.QuaternionUtils
import com.nexomc.nexo.utils.associateWithNotNull
import com.nexomc.nexo.utils.logs.Logs
import net.kyori.adventure.key.Key
import org.bukkit.entity.ItemDisplay
import org.bukkit.event.EventHandler
import org.bukkit.persistence.PersistentDataType

class AxiomCompatibility : CompatibilityProvider<AxiomPaper>() {

    @EventHandler
    fun NexoItemsLoadedEvent.onLoaded() {
        registerCustomBlocks()
        registerFurniture()
    }

    @EventHandler
    fun AxiomSpawnEntityEvent.onAxiomSpawn() {
        val baseEntity = entity as? ItemDisplay ?: return
        val mechanic = NexoFurniture.furnitureMechanic(baseEntity.itemStack) ?: return

        baseEntity.persistentDataContainer.set(FurnitureMechanic.FURNITURE_KEY, PersistentDataType.STRING, mechanic.itemID)
    }

    @EventHandler
    fun AxiomAfterManipulateEntityEvent.onAxiomManio() {
        val baseEntity = entity as? ItemDisplay ?: return
        val mechanic = NexoFurniture.furnitureMechanic(baseEntity) ?: return

        val yaw = -QuaternionUtils.leftRotationToYaw(baseEntity.transformation.leftRotation)
        baseEntity.transformation = baseEntity.transformation.apply {
            leftRotation.set(mechanic.properties.leftRotation)
        }
        baseEntity.setRotation(yaw, baseEntity.pitch)
        mechanic.hitbox.refreshHitboxes(baseEntity, mechanic)
        mechanic.light.refreshLights(baseEntity, mechanic)
        FurnitureSeat.updateSeats(baseEntity, mechanic)
        FurnitureBed.updateBeds(baseEntity, mechanic)
    }

    fun registerFurniture() {
        AxiomCustomDisplayAPI.getAPI().unregisterAll(NexoPlugin.instance())
        NexoFurniture.furnitureIDs().mapNotNull(NexoFurniture::furnitureMechanic).forEach { mechanic ->
            val nexoItem = NexoItems.itemFromId(mechanic.itemID)
            val searchKey = (nexoItem?.itemName ?: nexoItem?.displayName)?.let(AdventureUtils.MINI_MESSAGE::serialize) ?: mechanic.itemID
            val builder = AxiomCustomDisplayAPI.getAPI().create(Key.key("nexo:${mechanic.itemID}"), searchKey, nexoItem?.build())
            AxiomCustomDisplayAPI.getAPI().register(NexoPlugin.instance(), builder)
        }
    }

    data class DirectionalBlocks(val type: DirectionalBlock.DirectionalType, val blocks: List<NoteBlockMechanic>) {
        constructor(parent: DirectionalBlock) : this(parent.directionalType, listOf(
            parent.xBlock, parent.yBlock, parent.zBlock,
            parent.northBlock, parent.eastBlock, parent.southBlock, parent.westBlock,
            parent.upBlock, parent.downBlock
        ).mapNotNull(NexoBlocks::noteBlockMechanic))
    }

    fun registerCustomBlocks() {
        AxiomCustomBlocksAPI.getAPI().unregisterAll(NexoPlugin.instance())
        NexoBlocks.blockIDs().mapNotNull(NexoBlocks::customBlockMechanic).forEach { mechanic ->
            if (mechanic is NoteBlockMechanic && mechanic.directional != null) return@forEach

            val nexoItem = NexoItems.itemFromId(mechanic.itemID)
            val key = Key.key("nexo", mechanic.itemID)
            val translationKey = (nexoItem?.itemName ?: nexoItem?.displayName)?.let(AdventureUtils.MINI_MESSAGE::serialize) ?: mechanic.itemID
            val builder = AxiomCustomBlocksAPI.getAPI().createSingle(key, translationKey, mechanic.blockData)

            builder.preventShapeUpdates(true)
            builder.pickBlockItemStack(nexoItem?.build())
            AxiomCustomBlocksAPI.getAPI().register(NexoPlugin.instance(), builder)
        }

        NexoBlocks.noteBlockIDs().mapNotNull(NexoBlocks::noteBlockMechanic)
            .filter { it.directional?.isParentBlock() == true }
            .associateWithNotNull { DirectionalBlocks(it.directional!!) }
            .forEach { (mechanic, directional) ->
                val nexoItem = NexoItems.itemFromId(mechanic.itemID)
                val key = Key.key("nexo", mechanic.itemID)
                val translationKey = (nexoItem?.itemName ?: nexoItem?.displayName)?.let(AdventureUtils.MINI_MESSAGE::serialize) ?: mechanic.itemID

                val builder = when (directional.type) {
                    DirectionalBlock.DirectionalType.LOG -> {
                        val (x,y,z) = directional.blocks.take(3).map(NoteBlockMechanic::blockData)
                        AxiomCustomBlocksAPI.getAPI().createAxis(key, translationKey, x, y, z)
                    }
                    DirectionalBlock.DirectionalType.FURNACE -> {
                        val (n,e,s,w) = directional.blocks.take(4).map(NoteBlockMechanic::blockData)
                        AxiomCustomBlocksAPI.getAPI().createHorizontalFacing(key, translationKey, n, e, s, w)
                    }
                    DirectionalBlock.DirectionalType.DROPPER -> {
                        val (n,e,s,w) = directional.blocks.take(4).map(NoteBlockMechanic::blockData)
                        val (u,d) = directional.blocks.takeLast(2).map(NoteBlockMechanic::blockData)
                        AxiomCustomBlocksAPI.getAPI().createFacing(key, translationKey, n,e,s,w,u,d)
                    }
                }

                builder.preventShapeUpdates(true)
                builder.pickBlockItemStack(nexoItem?.build())
                AxiomCustomBlocksAPI.getAPI().register(NexoPlugin.instance(), builder)
            }
        Logs.logSuccess("Finished registering Nexo's Custom Blocks with Axiom!")
    }
}