package com.nexomc.nexo.compatibilities.mythicmobs

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.compatibilities.CompatibilityProvider
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.bukkit.adapters.item.ItemComponentBukkitItemStack
import io.lumine.mythic.bukkit.events.MythicDropLoadEvent
import io.lumine.mythic.bukkit.utils.numbers.RandomDouble
import io.lumine.mythic.core.drops.droppables.VanillaItemDrop
import org.bukkit.event.EventHandler

class MythicMobsCompatibility : CompatibilityProvider<MythicBukkit>() {
    @EventHandler
    fun MythicDropLoadEvent.onMythicDropLoadEvent() {
        if (!dropName.equals("nexo", true)) return

        val lines = container.line.split(" ")
        val itemId: String = if (lines.size == 4) lines[1] else if (lines.size == 3) lines[2] else ""
        val amountRange = RandomDouble(lines.firstOrNull { "-" in it } ?: "1-1")
        val nexoItem = NexoItems.itemFromId(itemId)?.build() ?: return

        register(VanillaItemDrop(container.line, config, ItemComponentBukkitItemStack(nexoItem), amountRange))
    }
}
