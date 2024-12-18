package com.nexomc.nexo.compatibilities.worldedit

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.configs.*
import com.nexomc.nexo.mechanics.custom_block.noteblock.NoteBlockMechanic
import com.nexomc.nexo.mechanics.custom_block.stringblock.StringBlockMechanic
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.utils.safeCast
import com.sk89q.jnbt.CompoundTag
import com.sk89q.jnbt.Tag
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.WorldEditException
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.entity.BaseEntity
import com.sk89q.worldedit.entity.Entity
import com.sk89q.worldedit.event.extent.EditSessionEvent
import com.sk89q.worldedit.extent.AbstractDelegateExtent
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.util.Location
import com.sk89q.worldedit.util.eventbus.Subscribe
import com.sk89q.worldedit.world.block.BlockStateHolder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.EntityType

class WorldEditHandlers(register: Boolean) {
    init {
        if (register) WorldEdit.getInstance().eventBus.register(this)
        else WorldEdit.getInstance().eventBus.unregister(this)
    }


    @Subscribe
    @Suppress("unused")
    fun EditSessionEvent.onEditSession() {
        if (world == null) return

        extent = object : AbstractDelegateExtent(extent) {
            override fun createEntity(location: Location, baseEntity: BaseEntity): Entity? {
                if (!Settings.WORLDEDIT_FURNITURE.toBool()) return super.createEntity(location, baseEntity)
                if (!baseEntity.hasNbtData() || baseEntity.type !== BukkitAdapter.adapt(EntityType.ITEM_DISPLAY))
                    return super.createEntity(location, baseEntity)
                getFurnitureMechanic(baseEntity) ?: return super.createEntity(location, baseEntity)

                // Remove interaction-tag from baseEntity-nbt
                val compoundTag = baseEntity.nbtData ?: return super.createEntity(location, baseEntity)
                val compoundTagMap = HashMap(compoundTag.value)
                val bukkitValues = compoundTagMap["BukkitValues"]?.value.safeCast<Map<String, Tag>>()?.toMutableMap() ?: mutableMapOf()
                bukkitValues.remove("nexo:interaction")
                compoundTagMap["BukkitValues"] = CompoundTag(bukkitValues)
                baseEntity.nbtData = CompoundTag(compoundTagMap)

                return super.createEntity(location, baseEntity)
            }

            @Throws(WorldEditException::class)
            override fun <T : BlockStateHolder<T>?> setBlock(pos: BlockVector3, block: T): Boolean {
                val blockData = BukkitAdapter.adapt(block)
                val world = Bukkit.getWorld(world?.name ?: return false)
                val loc = org.bukkit.Location(world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
                val mechanic = NexoBlocks.customBlockMechanic(blockData)
                when (blockData.material) {
                    Material.NOTE_BLOCK -> {
                        if (mechanic != null && Settings.WORLDEDIT_NOTEBLOCKS.toBool()) {
                            Bukkit.getScheduler().scheduleSyncDelayedTask(NexoPlugin.instance(), { NexoBlocks.place(mechanic.itemID, loc) }, 1L)
                        }
                    }
                    Material.TRIPWIRE -> {
                        if (mechanic != null && Settings.WORLDEDIT_STRINGBLOCKS.toBool()) {
                            Bukkit.getScheduler().scheduleSyncDelayedTask(NexoPlugin.instance(), { NexoBlocks.place(mechanic.itemID, loc) }, 1L)
                        }
                    }
                    else -> {
                        if (world == null) return super.setBlock(pos, block)
                        val replacingMechanic = NexoBlocks.customBlockMechanic(loc) ?: return super.setBlock(pos, block)
                        if (replacingMechanic is StringBlockMechanic && !Settings.WORLDEDIT_STRINGBLOCKS.toBool())
                            return super.setBlock(pos, block)
                        if (replacingMechanic is NoteBlockMechanic && !Settings.WORLDEDIT_NOTEBLOCKS.toBool())
                            return super.setBlock(pos, block)

                        Bukkit.getScheduler().scheduleSyncDelayedTask(NexoPlugin.instance(), { NexoFurniture.remove(loc) }, 1L)
                    }
                }

                return super.setBlock(pos, block)
            }

            fun getFurnitureMechanic(entity: BaseEntity): FurnitureMechanic? {
                if (!entity.hasNbtData() || entity.type !== BukkitAdapter.adapt(EntityType.ITEM_DISPLAY)) return null
                val tag = entity.nbtData ?: return null
                val bukkitValues = tag.value["BukkitValues"]?.value?.safeCast<Map<String, Tag>>() ?: return null
                val furnitureTag = bukkitValues["nexo:furniture"] ?: return null
                return NexoFurniture.furnitureMechanic(furnitureTag.value.toString())
            }
        }
    }
}
