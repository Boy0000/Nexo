package com.nexomc.nexo.compatibilities.mythicmobs

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.compatibilities.CompatibilityProvider
import com.nexomc.nexo.utils.logs.Logs
import io.lumine.mythic.api.adapters.AbstractItemStack
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.drops.DropMetadata
import io.lumine.mythic.bukkit.BukkitAdapter
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.bukkit.adapters.item.ItemComponentBukkitItemStack
import io.lumine.mythic.bukkit.events.MythicDropLoadEvent
import io.lumine.mythic.bukkit.utils.numbers.RandomDouble
import io.lumine.mythic.core.drops.droppables.VanillaItemDrop
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import kotlin.jvm.optionals.getOrNull

class MythicMobsCompatibility : CompatibilityProvider<MythicBukkit>() {
    @EventHandler
    fun MythicDropLoadEvent.onMythicDropLoadEvent() {
        if (dropName.lowercase() != "nexo") return

        val lines = container.line.lowercase().split(" ")
        val itemId = lines[1]
        val randomAmount = lines.getOrNull(2)?.takeIf { "-" in it } ?: "1-1"
        val noLooting = "nolooting" in lines
        val nexoItem = NexoItems.itemFromId(itemId)?.build() ?: return Logs.logWarn("Failed to load item $itemId in ${config.key}")

        register(NexoDrop(itemId, noLooting, container.line, container.config, ItemComponentBukkitItemStack(nexoItem), RandomDouble(randomAmount)))
    }
}

class NexoDrop(
    val itemId: String, val noLooting: Boolean, line: String, config: MythicLineConfig, item: AbstractItemStack, val randomAmount: RandomDouble,
) : VanillaItemDrop(line, config, item, randomAmount) {

    override fun getDrop(metadata: DropMetadata?, amount: Double): AbstractItemStack? {
        val itemInKillerHand = (BukkitAdapter.adapt(metadata?.cause?.getOrNull()) as? Player)?.inventory?.itemInMainHand
        val lootingLvl = if (noLooting) 0 else itemInKillerHand?.enchantments?.get(Enchantment.LOOTING) ?: 0
        val amount = (randomAmount.min.toInt()..(randomAmount.max.toInt() + lootingLvl)).random().toDouble()

        return super.getDrop(metadata, amount)
    }
}
