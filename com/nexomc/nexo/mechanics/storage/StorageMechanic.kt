package com.nexomc.nexo.mechanics.storage

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.mechanics.furniture.FurnitureHelpers
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.utils.AdventureUtils
import com.nexomc.nexo.utils.BlockHelpers.getPersistentDataContainer
import com.nexomc.nexo.utils.BlockHelpers.isLoaded
import com.nexomc.nexo.utils.ItemUtils.displayName
import com.jeff_media.morepersistentdatatypes.DataType
import com.ticxo.modelengine.api.ModelEngineAPI
import com.ticxo.modelengine.api.model.ActiveModel
import com.willfp.eco.core.data.get
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.StorageGui
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Entity
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.util.*

class StorageMechanic(section: ConfigurationSection) {
    private val rows: Int = section.getInt("rows", 6)
    private val title: String = section.getString("title", "Storage")!!
    val storageType: StorageType = StorageType.valueOf(section.getString("type", "STORAGE")!!)
    private val openSound: String = section.getString("open_sound", "minecraft:block.chest.open")!!
    private val closeSound: String = section.getString("close_sound", "minecraft:block.chest.close")!!
    private val openAnimation: String? = section.getString("open_animation", null)
    private val closeAnimation: String? = section.getString("close_animation", null)
    private val volume: Float = section.getDouble("volume", 0.5).toFloat()
    private val pitch: Float = section.getDouble("pitch", 0.95).toFloat()

    fun openPersonalStorage(player: Player, location: Location, baseEntity: ItemDisplay?) {
        if (storageType != StorageType.PERSONAL) return
        createPersonalGui(player, baseEntity).open(player)
        if (baseEntity != null) playOpenAnimation(baseEntity, openAnimation)
        if (location.isWorldLoaded()) location.world.playSound(location, openSound, volume, pitch)
    }

    fun openDisposal(player: Player, location: Location, baseEntity: ItemDisplay?) {
        if (storageType != StorageType.DISPOSAL) return
        createDisposalGui(location, baseEntity).open(player)
        if (baseEntity != null) playOpenAnimation(baseEntity, openAnimation)
        if (location.isWorldLoaded()) location.world.playSound(location, openSound, volume, pitch)
    }

    fun openStorage(block: Block, player: Player) {
        if (block.type != Material.NOTE_BLOCK) return
        (blockStorages[block] ?: createGui(block).apply { blockStorages[block] = this }).open(player)
        if (block.location.isWorldLoaded()) block.world.playSound(block.location, openSound, volume, pitch)
    }

    fun openStorage(baseEntity: ItemDisplay, player: Player) {
        (displayStorages[baseEntity] ?: createGui(baseEntity)?.apply {
            displayStorages[baseEntity] = this
        })?.open(player)
        playOpenAnimation(baseEntity, openAnimation)
        if (baseEntity.location.isWorldLoaded()) baseEntity.world.playSound(baseEntity.location, openSound, volume, pitch)
    }

    private fun playOpenAnimation(baseEntity: ItemDisplay?, animation: String?) {
        if (baseEntity == null || animation == null) return
        val uuid = baseEntity.persistentDataContainer.get(FurnitureMechanic.MODELENGINE_KEY, DataType.UUID) ?: return

        ModelEngineAPI.getModeledEntity(uuid)?.models?.values?.forEach { model: ActiveModel ->
            model.animationHandler.forceStopAllAnimations()
            model.animationHandler.playAnimation(animation, 0.0, 0.0, 1.0, true)
        }
    }

    fun dropStorageContent(block: Block) {
        val gui = blockStorages[block]
        val pdc = getPersistentDataContainer(block)
        // If shutdown the gui isn't saved and map is empty, so use pdc storage
        val items = when {
            block in blockStorages && gui != null -> gui.inventory.contents
            else -> pdc.getOrDefault(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, arrayOf<ItemStack>())
        }

        if (isShulker) {
            val mechanic = NexoBlocks.noteBlockMechanic(block) ?: return
            val shulker = NexoItems.itemFromId(mechanic.itemID)?.build() ?: return
            shulker.editMeta {
                it.persistentDataContainer.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, items)
            }
            block.world.dropItemNaturally(block.location, shulker)
        } else items.filterNotNull().forEach { block.world.dropItemNaturally(block.location, it) }
        gui?.inventory?.viewers?.filterIsInstance<Player>()?.forEach(gui::close)
        pdc.remove(STORAGE_KEY)
        blockStorages.remove(block)
    }

    fun dropStorageContent(mechanic: FurnitureMechanic, baseEntity: ItemDisplay) {
        val gui = displayStorages[baseEntity]
        val pdc = baseEntity.persistentDataContainer
        // If shutdown the gui isn't saved and map is empty, so use pdc storage
        val items = when {
            baseEntity in displayStorages && gui != null -> gui.inventory.contents
            else -> pdc.getOrDefault(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, arrayOf<ItemStack>())
        }
        
        if (isShulker) {
            val defaultItem = NexoItems.itemFromId(mechanic.itemID)!!.build()
            val shulker = FurnitureHelpers.furnitureItem(baseEntity)
            val shulkerMeta = shulker!!.itemMeta

            if (shulkerMeta != null) {
                shulkerMeta.persistentDataContainer.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, items)
                displayName(shulkerMeta, defaultItem.itemMeta)
                shulker.itemMeta = shulkerMeta
            }
            baseEntity.world.dropItemNaturally(baseEntity.location, shulker)
        } else items.filterNotNull().forEach {
            baseEntity.world.dropItemNaturally(baseEntity.location, it)
        }

        if (gui != null) {
            val players = gui.inventory.viewers.toTypedArray<HumanEntity>()
            for (player: HumanEntity in players) gui.close(player)
        }
        pdc.remove(STORAGE_KEY)
        displayStorages.remove(baseEntity)
    }

    val isStorage: Boolean
        get() = storageType == StorageType.STORAGE

    val isPersonal: Boolean
        get() = storageType == StorageType.PERSONAL

    val isEnderchest: Boolean
        get() = storageType == StorageType.ENDERCHEST

    val isDisposal: Boolean
        get() = storageType == StorageType.DISPOSAL

    val isShulker: Boolean
        get() = storageType == StorageType.SHULKER

    private fun createDisposalGui(location: Location, baseEntity: ItemDisplay?): StorageGui {
        val gui = Gui.storage().title(AdventureUtils.MINI_MESSAGE.deserialize(title)).rows(rows).create()

        gui.setOpenGuiAction { gui.inventory.clear() }

        gui.setCloseGuiAction {
            gui.inventory.clear()
            if (location.isWorldLoaded()) location.world.playSound(location, closeSound, volume, pitch)
            if (baseEntity != null) playOpenAnimation(baseEntity, closeAnimation)
        }
        return gui
    }

    private fun createPersonalGui(player: Player, baseEntity: ItemDisplay?): StorageGui {
        val storagePDC = player.persistentDataContainer
        val gui = Gui.storage().title(AdventureUtils.MINI_MESSAGE.deserialize(title)).rows(rows).create()

        // Slight delay to catch stacks sometimes moving too fast
        gui.setDefaultClickAction { event: InventoryClickEvent ->
            if (event.cursor.type != Material.AIR || event.getCurrentItem() != null) {
                Bukkit.getScheduler().runTaskLater(
                    NexoPlugin.instance(),
                    Runnable {
                        storagePDC.set(
                            STORAGE_KEY,
                            DataType.ITEM_STACK_ARRAY,
                            gui.inventory.contents
                        )
                    }, 3L
                )
            }
        }

        gui.setOpenGuiAction {
            playerStorages.add(player)
            if (storagePDC.has(PERSONAL_STORAGE_KEY, DataType.ITEM_STACK_ARRAY)) gui.inventory.contents = Objects.requireNonNull(
                storagePDC.get(
                    PERSONAL_STORAGE_KEY, DataType.ITEM_STACK_ARRAY
                )
            )
        }

        gui.setCloseGuiAction {
            playerStorages.remove(player)
            storagePDC.set(PERSONAL_STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.inventory.contents)
            if (player.location.isWorldLoaded()) player.location.world.playSound(player.location, closeSound, volume, pitch)
            if (baseEntity != null) playOpenAnimation(baseEntity, closeAnimation)
        }

        return gui
    }

    private fun createGui(block: Block): StorageGui {
        val location = block.location
        val storagePDC = getPersistentDataContainer(block)
        val gui = Gui.storage().title(AdventureUtils.MINI_MESSAGE.deserialize(title)).rows(rows).create()

        // Slight delay to catch stacks sometimes moving too fast
        gui.setDefaultClickAction { event: InventoryClickEvent ->
            if (event.cursor.type != Material.AIR || event.getCurrentItem() != null) {
                Bukkit.getScheduler().runTaskLater(
                    NexoPlugin.instance(),
                    Runnable {
                        storagePDC.set(
                            STORAGE_KEY,
                            DataType.ITEM_STACK_ARRAY,
                            gui.inventory.contents
                        )
                    }, 3L
                )
            }
        }
        gui.setOpenGuiAction {
            if (storagePDC.has(
                    STORAGE_KEY, DataType.ITEM_STACK_ARRAY
                )
            ) gui.inventory.contents = storagePDC.getOrDefault(
                STORAGE_KEY, DataType.ITEM_STACK_ARRAY, arrayOf<ItemStack>()
            )
        }

        gui.setCloseGuiAction {
            storagePDC.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.inventory.contents)
            if (isLoaded(block.location)) location.world.playSound(location, closeSound, volume, pitch)
        }

        return gui
    }

    private fun createGui(baseEntity: ItemDisplay): StorageGui? {
        val location = baseEntity.location
        val furnitureItem = FurnitureHelpers.furnitureItem(baseEntity) ?: return null
        val storagePDC = baseEntity.persistentDataContainer
        val itemPDC = furnitureItem.itemMeta.persistentDataContainer
        val shulker = isShulker
        val shulkerPDC = if (shulker) itemPDC else null
        val gui = Gui.storage().title(AdventureUtils.MINI_MESSAGE.deserialize(title)).rows(rows).create()

        // Slight delay to catch stacks sometimes moving too fast
        gui.setDefaultClickAction { event: InventoryClickEvent ->
            if (event.cursor.type != Material.AIR || event.getCurrentItem() != null) {
                Bukkit.getScheduler().runTaskLater(
                    NexoPlugin.instance(),
                    Runnable {
                        storagePDC.set(
                            STORAGE_KEY,
                            DataType.ITEM_STACK_ARRAY,
                            gui.inventory.contents
                        )
                    }, 3L
                )
            }
        }

        // If it's a shulker, get the itemstack array of the items pdc, otherwise use the frame pdc
        gui.setOpenGuiAction {
            gui.inventory.contents = (if (!shulker && storagePDC.has(STORAGE_KEY, DataType.ITEM_STACK_ARRAY))
                storagePDC.getOrDefault(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, arrayOf<ItemStack>())
            else
                if (shulker && shulkerPDC!!.has(STORAGE_KEY, DataType.ITEM_STACK_ARRAY))
                    shulkerPDC.getOrDefault(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, arrayOf())
                else
                    arrayOf<ItemStack>())
        }

        gui.setCloseGuiAction {
            if (gui.inventory.viewers.size <= 1) {
                when {
                    shulker -> shulkerPDC!!.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.inventory.contents)
                    else -> storagePDC.set(STORAGE_KEY, DataType.ITEM_STACK_ARRAY, gui.inventory.contents)
                }
            }
            if (isLoaded(baseEntity.location)) location.world.playSound(location, closeSound, volume, pitch)
            playOpenAnimation(baseEntity, closeAnimation)
        }

        return gui
    }

    companion object {
        var playerStorages = mutableSetOf<Player>()
        var blockStorages = mutableMapOf<Block, StorageGui>()
        var displayStorages = mutableMapOf<ItemDisplay, StorageGui>()
        val STORAGE_KEY = NamespacedKey(NexoPlugin.instance(), "storage")
        val PERSONAL_STORAGE_KEY = NamespacedKey(NexoPlugin.instance(), "personal_storage")
    }
}
