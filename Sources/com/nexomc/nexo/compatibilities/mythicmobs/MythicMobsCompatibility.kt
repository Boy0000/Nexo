package com.nexomc.nexo.compatibilities.mythicmobs

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.compatibilities.CompatibilityProvider
import io.lumine.mythic.api.adapters.AbstractItemStack
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.drops.DropMetadata
import io.lumine.mythic.api.drops.IItemDrop
import io.lumine.mythic.api.skills.placeholders.PlaceholderDouble
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.bukkit.adapters.item.ItemComponentBukkitItemStack
import io.lumine.mythic.bukkit.events.MythicDropLoadEvent
import io.lumine.mythic.bukkit.utils.numbers.Numbers
import io.lumine.mythic.core.drops.droppables.ItemDrop
import io.lumine.mythic.core.glow.GlowColor
import io.lumine.mythic.core.logging.MythicLogger
import java.util.Optional
import org.bukkit.event.EventHandler
import org.bukkit.inventory.ItemStack

class MythicMobsCompatibility : CompatibilityProvider<MythicBukkit>() {
    @EventHandler
    fun MythicDropLoadEvent.onMythicDropLoadEvent() {
        if (dropName.lowercase() != "nexo") return
        val itemId = container.line.lowercase().split(" ").getOrNull(1) ?: return
        val item = NexoItems.itemFromId(itemId)?.build() ?: return
        register(NexoDrop(container.line, item, config))
    }
}

class NexoDrop(line: String, item: ItemStack, config: MythicLineConfig) : ItemDrop(line, config), IItemDrop {
    val item = ItemComponentBukkitItemStack(item)
    val level: PlaceholderDouble? = config.getPlaceholderDouble(arrayOf("level", "lvl", "l"), "null", *arrayOfNulls<String>(0))

    init {
        config.getString(arrayOf("lootsplosion", "lootsplosionenabled", "ls"), null, *arrayOfNulls<String>(0))?.toBooleanStrictOrNull()?.let {
            this.lootsplosionEnabled = Optional.of(it)
        }

        config.getString(arrayOf("itemvfx", "itemvfxenabled", "iv"), null, *arrayOfNulls<String>(0))?.toBooleanStrictOrNull()?.let {
            this.itemVFXEnabled = Optional.of(it)
        }

        this.itemVfxMaterial = config.getString(arrayOf<String>("itemvfxmaterial", "itemvfxmaterial", "ivm"), null, *arrayOfNulls<String>(0))
        this.itemVfxData = config.getInteger(arrayOf<String>("vfxdata", "vfxd"), 0)
        this.vfxColor = config.getString(arrayOf<String>("vfxcolor", "vfxc", "color"), null, *arrayOfNulls<String>(0))

        config.getString(arrayOf("hologramname", "hologramnameenabled", "hn"), null, *arrayOfNulls<String>(0))?.toBooleanStrictOrNull()?.let {
            this.hologramNameEnabled = Optional.of(it)
        }

        config.getString(arrayOf("clientsidedrops", "clientsidedropsenabled", "csd"), null, *arrayOfNulls<String>(0))?.toBooleanStrictOrNull()?.let {
            this.clientSideDropsEnabled = Optional.of(it)
        }

        config.getString(arrayOf<String>("itemglowcolor", "glowcolor", "gc"), null, *arrayOfNulls<String>(0))?.let { color ->
            runCatching {
                this.itemGlow = GlowColor.valueOf(color)
                this.itemGlowEnabled = Optional.of(true)
            }.onFailure {
                MythicLogger.errorDropConfig(this, config, "Invalid ItemGlow Color specified")
                it.printStackTrace()
            }
        }

        config.getColor(arrayOf("itembeamcolor", "beamColor", "bc"), null)?.let { beamColor ->
            runCatching {
                this.itemBeam = beamColor
                this.itemBeamEnabled = Optional.of<Boolean>(true)
            }.onFailure {
                MythicLogger.errorDropConfig(this, config, "Invalid BeamColor Color specified")
            }
        }

        this.displayBillboarding = config.getString(arrayOf<String>("billboarding", "billboard", "bill"), "VERTICAL", *arrayOfNulls<String>(0))
        this.displayBrightness = config.getInteger(arrayOf<String>("brightness", "bright", "b"), 0)
    }

    override fun getDrop(meta: DropMetadata, amount: Double): AbstractItemStack? {
        val finalAmount = this.rollBonuses(meta, Numbers.floor(((this.item.amount.toDouble() * amount).toInt()).toDouble()))
        return this.item.copy().amount(finalAmount)
    }
}
