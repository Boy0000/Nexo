package com.nexomc.nexo.mechanics.misc.backpack

import com.jeff_media.morepersistentdatatypes.DataType
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.utils.AdventureUtils
import com.nexomc.nexo.utils.InventoryUtils.topInventoryForPlayer
import com.nexomc.nexo.utils.ItemUtils
import com.nexomc.nexo.utils.SchedulerUtils
import com.nexomc.nexo.utils.VersionUtil
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.StorageGui
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

class BackpackListener(private val factory: BackpackMechanicFactory) : Listener {
    // Cancels placing backpacks if the base material is a block
    @EventHandler(priority = EventPriority.MONITOR)
    fun BlockPlaceEvent.onBlockPlace() {
        if (isBackpack(player.inventory.itemInMainHand)) isCancelled = true
    }

    @EventHandler
    fun PlayerDropItemEvent.onDrop() {
        if (isBackpack(itemDrop.itemStack)) closeBackpack(player)
    }

    @EventHandler
    fun PlayerQuitEvent.onPlayerDisconnect() {
        closeBackpack(player)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun PlayerInteractEvent.onPlayerInteract() {
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return
        if (hand == EquipmentSlot.OFF_HAND || useItemInHand() == Event.Result.DENY) return
        setUseItemInHand(Event.Result.ALLOW)
        openBackpack(player)
    }

    @EventHandler
    fun PlayerSwapHandItemsEvent.onPlayerSwapHandItems() {
        if (!isBackpack(offHandItem) && !isBackpack(mainHandItem)) return
        (topInventoryForPlayer(player).holder as? StorageGui)?.close(player, true)
    }

    // Refresh close backpack if open to refresh with picked up items
    @EventHandler(ignoreCancelled = true)
    fun EntityPickupItemEvent.onPickupItem() {
        val player = entity as? Player ?: return
        if (topInventoryForPlayer(player).holder !is Gui) return
        closeBackpack(player)
        openBackpack(player)
    }

    private fun createGUI(mechanic: BackpackMechanic, backpack: ItemStack): StorageGui? {
        val backpackMeta = backpack.itemMeta
        if (!isBackpack(backpack) || backpackMeta == null) return null
        val pdc = backpackMeta.persistentDataContainer
        val gui = Gui.storage().title(AdventureUtils.MINI_MESSAGE.deserialize(mechanic.title)).rows(mechanic.rows).create()

        gui.disableItemDrop()
        gui.disableItemSwap()

        gui.setPlayerInventoryAction { event: InventoryClickEvent ->
            if (isBackpack(event.currentItem) || isBackpack(event.cursor)) event.isCancelled = true
        }

        gui.setDefaultClickAction { event: InventoryClickEvent ->
            if (isBackpack(event.currentItem) || isBackpack(event.cursor)) event.isCancelled = true
            if (!event.cursor.isEmpty || event.currentItem?.isEmpty == false) SchedulerUtils.runTaskLater(1L) {
                pdc.set(BackpackMechanic.BACKPACK_KEY, DataType.ITEM_STACK_ARRAY, gui.inventory.contents)
                backpack.itemMeta = backpackMeta
            }
        }

        gui.setDragAction { event: InventoryDragEvent ->
            if (isBackpack(event.cursor)) event.isCancelled = true
        }

        gui.setOutsideClickAction { event: InventoryClickEvent ->
            if (isBackpack(event.cursor)) event.isCancelled = true
        }

        gui.setOpenGuiAction { event: InventoryOpenEvent ->
            val player = event.player as Player
            val contents = pdc.get(BackpackMechanic.BACKPACK_KEY, DataType.ITEM_STACK_ARRAY)
            if (contents != null) gui.inventory.contents = contents
            player.playSound(player.location, mechanic.openSound, mechanic.volume, mechanic.pitch)
        }

        gui.setCloseGuiAction { event: InventoryCloseEvent ->
            val player = event.player as Player
            pdc.set(BackpackMechanic.BACKPACK_KEY, DataType.ITEM_STACK_ARRAY, gui.inventory.contents)
            backpack.itemMeta = backpackMeta
            player.world.playSound(player.location, mechanic.closeSound, mechanic.volume, mechanic.pitch)
        }

        return gui
    }

    private fun isBackpack(item: ItemStack?): Boolean {
        return item != null && item.type != Material.BUNDLE && factory.getMechanic(NexoItems.idFromItem(item)) != null
    }

    private fun openBackpack(player: Player) {
        val itemInHand = player.inventory.itemInMainHand.takeIf { it.type != Material.BUNDLE } ?: return
        val mechanic = factory.getMechanic(NexoItems.idFromItem(itemInHand)) ?: return
        if (VersionUtil.atleast("1.21.2") && player.hasCooldown(itemInHand)) return

        ItemUtils.triggerCooldown(player, itemInHand)

        createGUI(mechanic, itemInHand)?.open(player)
    }

    private fun closeBackpack(player: Player) {
        val holder = topInventoryForPlayer(player).holder
        if (!isBackpack(player.inventory.itemInMainHand)) return
        if (holder is StorageGui) holder.close(player, true)
    }
}
