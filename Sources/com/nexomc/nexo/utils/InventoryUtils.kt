package com.nexomc.nexo.utils

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryView

object InventoryUtils {
    private var getTitleMethod = runCatching { InventoryView::class.java.getDeclaredMethod("getTitle") }.getOrNull()
    private var titleMethod = runCatching { InventoryView::class.java.getDeclaredMethod("title") }.getOrNull()
    private var topInventoryMethod = runCatching { InventoryView::class.java.getDeclaredMethod("getTopInventory") }.getOrNull()
    private var playerFromViewMethod = runCatching { InventoryView::class.java.getDeclaredMethod("getPlayer") }.getOrNull()

    fun titleFromView(event: InventoryEvent): Component {
        if (VersionUtil.atleast("1.21")) return event.view.title()
        return runCatching {
            titleMethod!!.invoke(event.view) as Component
        }.printOnFailure(true).getOrNull() ?: Component.empty()
    }

    @JvmStatic
    fun playerFromView(event: InventoryEvent): Player? {
        if (VersionUtil.atleast("1.21")) return event.view.player as Player
        return runCatching {
            playerFromViewMethod!!.invoke(event.view) as Player
        }.printOnFailure(true).getOrNull()
    }

    @JvmStatic
    fun getTitleFromView(event: InventoryEvent): String {
        if (VersionUtil.atleast("1.21")) return event.view.title
        return runCatching {
            getTitleMethod!!.invoke(event.view) as String
        }.printOnFailure(true).getOrNull() ?: ""
    }

    @JvmStatic
    fun topInventoryForPlayer(player: Player): Inventory {
        if (VersionUtil.atleast("1.21")) return player.openInventory.topInventory
        return runCatching {
            topInventoryMethod!!.invoke(player.openInventory) as Inventory
        }.printOnFailure(true).getOrNull() ?: player.inventory
    }
}
