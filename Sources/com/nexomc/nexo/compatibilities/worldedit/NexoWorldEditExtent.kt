package com.nexomc.nexo.compatibilities.worldedit

import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.compatibilities.worldedit.CustomBlocksWorldEditUtils.processBlock
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.utils.SchedulerUtils
import com.nexomc.nexo.utils.safeCast
import com.sk89q.jnbt.Tag
import com.sk89q.worldedit.WorldEditException
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.entity.BaseEntity
import com.sk89q.worldedit.entity.Entity
import com.sk89q.worldedit.extent.AbstractDelegateExtent
import com.sk89q.worldedit.extent.Extent
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.util.Location
import com.sk89q.worldedit.world.block.BlockState
import com.sk89q.worldedit.world.block.BlockStateHolder
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemDisplay
import java.util.*

class NexoWorldEditExtent(extent: Extent, val world: World) : AbstractDelegateExtent(extent) {

    override fun createEntity(location: Location, baseEntity: BaseEntity): Entity? {
        if (!Settings.WORLDEDIT_FURNITURE.toBool()) return super.createEntity(location, baseEntity)

        // Do not copy FurnitureSeat entities
        if (baseEntity.type == BukkitAdapter.adapt(EntityType.INTERACTION)) {
            return if (baseEntity.nbtData?.value?.get("BukkitValues")?.value.safeCast<Map<String, Any>>()?.containsKey("nexo:seat") == true) null
            else super.createEntity(location, baseEntity)
        }

        // If the entity is not Furniture or FurnitureSeat, return
        if (!baseEntity.hasNbtData() || baseEntity.type !== BukkitAdapter.adapt(EntityType.ITEM_DISPLAY))
            return super.createEntity(location, baseEntity)

        val mechanic = furnitureMechanic(baseEntity) ?: return super.createEntity(location, baseEntity)
        val originalUUID = baseEntity.nbtData!!.getIntArray("UUID").let { intArray ->
            UUID(
                (intArray[0].toLong() shl 32) or (intArray[1].toLong() and 0xFFFFFFFF),
                (intArray[2].toLong() shl 32) or (intArray[3].toLong() and 0xFFFFFFFF)
            )
        }

        SchedulerUtils.syncDelayedTask(2L) {
            Bukkit.getEntity(originalUUID)?.safeCast<ItemDisplay>()?.also(NexoFurniture::updateFurniture)
            val location = BukkitAdapter.adapt(world, location)
            mechanic.place(location, location.yaw, BlockFace.UP, false)
        }

        return null
    }

    @Throws(WorldEditException::class)
    override fun <T : BlockStateHolder<T>?> setBlock(pos: BlockVector3, stateToSet: T): Boolean {
        if (!Settings.WORLDEDIT_CUSTOM_BLOCKS.toBool()) return super.setBlock(pos, stateToSet)
        val location = BukkitAdapter.adapt(this.world, pos)

        processBlock(location, stateToSet as BlockState)

        return super.setBlock<T>(pos, stateToSet)
    }

    private fun furnitureMechanic(entity: BaseEntity): FurnitureMechanic? {
        if (!entity.hasNbtData() || entity.type !== BukkitAdapter.adapt(EntityType.ITEM_DISPLAY)) return null
        val tag = entity.nbtData ?: return null
        val bukkitValues = tag.value["BukkitValues"]?.value?.safeCast<Map<String, Tag<*,*>>>() ?: return null
        val furnitureTag = bukkitValues["nexo:furniture"] ?: return null
        return NexoFurniture.furnitureMechanic(furnitureTag.value.toString())
    }
}