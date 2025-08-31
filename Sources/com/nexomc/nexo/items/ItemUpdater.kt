package com.nexomc.nexo.items

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import com.jeff_media.morepersistentdatatypes.DataType
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.nms.NMSHandlers
import com.nexomc.nexo.utils.AdventureUtils
import com.nexomc.nexo.utils.ItemUtils
import com.nexomc.nexo.utils.SchedulerUtils
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.asColorable
import com.nexomc.nexo.utils.printOnFailure
import com.nexomc.nexo.utils.serialize
import com.nexomc.nexo.utils.ticks
import io.papermc.paper.datacomponent.DataComponentTypes
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.enchantment.PrepareItemEnchantEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ArmorMeta
import org.bukkit.inventory.meta.BundleMeta
import org.bukkit.inventory.meta.Damageable
import org.bukkit.persistence.PersistentDataType

@Suppress("UnstableApiUsage")
class ItemUpdater : Listener {

    init {
        SchedulerUtils.launchDelayed(2.ticks) {
            if (Settings.UPDATE_ENTITY_CONTENTS.toBool()) SchedulerUtils.runAtWorldEntities(::updateEntityInventories)
            if (Settings.UPDATE_TILE_ENTITY_CONTENTS.toBool()) SchedulerUtils.runAtWorldTileStates({ it.type in TILE_ENTITIES }) { tileEntity ->
                (tileEntity as? InventoryHolder)?.inventory?.contents?.forEachIndexed { index, item ->
                    if (item != null) tileEntity.inventory.setItem(index, updateItem(item))
                }
            }
        }
    }

    @EventHandler
    fun EntityAddToWorldEvent.onEntityLoad() {
        if (Settings.UPDATE_ENTITY_CONTENTS.toBool()) SchedulerUtils.launchDelayed(entity, 2.ticks) {
            updateEntityInventories(entity)
        }
    }

    @EventHandler
    fun ChunkLoadEvent.onChunkLoad() {
        if (Settings.UPDATE_TILE_ENTITY_CONTENTS.toBool() && !isNewChunk) chunk.getTileEntities({ it.type in TILE_ENTITIES }, false).forEach { tileEntity ->
            val inventory = (tileEntity as? InventoryHolder)?.inventory ?: return@forEach
            inventory.contents.forEachIndexed { index, item ->
                if (item != null) inventory.setItem(index, updateItem(item))
            }
        }
    }

    @EventHandler
    fun EntityPickupItemEvent.onPlayerPickUp() {
        if (!Settings.UPDATE_ITEMS.toBool() || entity !is Player) return

        val (oldItem, newItem) = item.itemStack to updateItem(item.itemStack)
        if (oldItem == newItem) return
        item.itemStack = newItem
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun PrepareItemEnchantEvent.onItemEnchant() {
        NexoItems.itemFromId(NexoItems.idFromItem(item))?.nexoMeta?.takeIf(NexoMeta::disableEnchanting)?.apply {
            isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun PrepareAnvilEvent.onItemEnchant() {
        val item = inventory.getItem(0) ?: return

        NexoItems.builderFromItem(item)?.nexoMeta?.takeIf(NexoMeta::disableEnchanting)?.apply {
            if (result?.enchantments != item.enchantments) result = null
        }
    }

    // Until Paper changes getReplacement to use food-component, this is the best way
    @EventHandler(ignoreCancelled = true)
    fun PlayerItemConsumeEvent.onUseConvertedTo() {
        val itemMeta = item.itemMeta ?: return
        if (!VersionUtil.atleast("1.21")) return
        val usingConvertsTo = ItemUtils.getUsingConvertsTo(itemMeta) ?: return
        if (!item.isSimilar(updateItem(usingConvertsTo))) return

        val inventory = player.inventory
        if (inventory.firstEmpty() == -1) setItem(item.add(usingConvertsTo.amount))
        else SchedulerUtils.launch(player) {
            for (i in 0..inventory.size) {
                val oldItem = inventory.getItem(i) ?: continue
                val newItem = updateItem(oldItem).takeIf(item::isSimilar) ?: continue

                // Remove the item and add it to fix stacking
                inventory.setItem(i, null)
                inventory.addItem(newItem)
            }
        }
    }

    companion object {
        private val IF_UUID = NamespacedKey.fromString("nexo:if-uuid")!!
        private val MF_GUI = NamespacedKey.fromString("nexo:mf-gui")!!

        val TILE_ENTITIES = arrayOf(
            Material.BARREL, Material.CHEST, Material.TRAPPED_CHEST, Material.CRAFTER, Material.FURNACE_MINECART, Material.DECORATED_POT,
            Material.HOPPER, Material.DROPPER, Material.DISPENSER, Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
            Material.SMOKER, Material.FURNACE, Material.BLAST_FURNACE, Material.BREWING_STAND, Material.JUKEBOX,
        ).plus(Tag.SHULKER_BOXES.values)

        fun updateEntityInventories(entity: Entity) {
            if (entity is ItemFrame) entity.setItem(updateItem(entity.item), false)
            if (entity is ItemDisplay) entity.setItemStack(updateItem(entity.itemStack))
            if (entity is Item) entity.itemStack = updateItem(entity.itemStack)
            if (entity is InventoryHolder) entity.inventory.contents.forEachIndexed { i, content ->
                if (content != null) entity.inventory.setItem(i, updateItem(content))
            }
            if (entity is LivingEntity) entity.equipment?.also { equipment ->
                EquipmentSlot.entries.forEach slot@{ slot ->
                    runCatching { equipment.getItem(slot).let(::updateItem) }.onSuccess {
                        equipment.setItem(slot, it)
                    }
                }
            }
        }

        fun updateItem(oldItem: ItemStack): ItemStack {

            // ItemsAdder does fucky stuff with PDC-entry so we need to use NMS for it
            if (NexoPlugin.instance().converter().itemsadderConverter.convertItems)
                NMSHandlers.handler().pluginConverter.convertItemsAdder(oldItem)
            if (NexoPlugin.instance().converter().oraxenConverter.convertItems)
                NMSHandlers.handler().pluginConverter.convertOraxen(oldItem)

            val id = NexoItems.idFromItem(oldItem) ?: return oldItem

            ItemUtils.editPersistentDataContainer(oldItem) {
                it.remove(IF_UUID)
                it.remove(MF_GUI)
            }

            val newItemBuilder = NexoItems.itemFromId(id)?.takeUnless { it.nexoMeta?.noUpdate == true } ?: return oldItem
            val newItem = NMSHandlers.handler().itemUtils().copyItemNBTTags(oldItem, newItemBuilder.build().clone())
            newItem.amount = oldItem.amount

            newItem.editMeta { itemMeta ->
                val (oldMeta, newMeta) = (oldItem.itemMeta ?: return@editMeta) to (newItem.itemMeta ?: return@editMeta)
                val (oldPdc, itemPdc) = oldMeta.persistentDataContainer to itemMeta.persistentDataContainer

                oldPdc.copyTo(itemPdc, true)

                oldMeta.enchants.entries.forEach { (enchant, level) ->
                    itemMeta.addEnchant(enchant, level, true)
                }
                newMeta.enchants.entries.filterNot { oldMeta.enchants.containsKey(it.key) }.forEach { (enchant, level) ->
                    itemMeta.addEnchant(enchant, level, true)
                }

                runCatching {
                    when {
                        newMeta.hasCustomModelData() -> newMeta.customModelData
                        oldMeta.hasCustomModelData() -> oldMeta.customModelData
                        else -> null
                    }.let(itemMeta::setCustomModelData)
                }.printOnFailure(true)

                when {
                    !oldMeta.hasLore() || Settings.OVERRIDE_ITEM_LORE.toBool() -> newMeta.lore()
                    else -> oldMeta.lore()
                }.let(itemMeta::lore)

                if (newMeta.hasAttributeModifiers()) itemMeta.attributeModifiers = newMeta.attributeModifiers
                else if (oldMeta.hasAttributeModifiers()) itemMeta.attributeModifiers = oldMeta.attributeModifiers

                val maxDamage = (itemMeta as? Damageable)?.takeIf { VersionUtil.atleast("1.20.6") && it.hasMaxDamage() }?.maxDamage ?: newItem.type.maxDurability.toInt()
                if (itemMeta is Damageable && oldMeta is Damageable && oldMeta.hasDamage()) itemMeta.damage = oldMeta.damage.coerceAtMost(maxDamage)

                if (oldMeta.isUnbreakable) itemMeta.isUnbreakable = true

                (itemMeta as? BundleMeta)?.setItems((oldMeta as? BundleMeta)?.items)

                if (itemMeta is ArmorMeta) when {
                    newMeta is ArmorMeta && newMeta.hasTrim() -> itemMeta.trim = newMeta.trim
                    oldMeta is ArmorMeta && oldMeta.hasTrim() -> itemMeta.trim = oldMeta.trim
                }

                if (VersionUtil.atleast("1.20.5")) {
                    when {
                        newMeta.hasFood() -> itemMeta.setFood(newMeta.food)
                        oldMeta.hasFood() -> itemMeta.setFood(oldMeta.food)
                    }

                    when {
                        newMeta.hasEnchantmentGlintOverride() -> itemMeta.setEnchantmentGlintOverride(newMeta.enchantmentGlintOverride)
                        oldMeta.hasEnchantmentGlintOverride() -> itemMeta.setEnchantmentGlintOverride(oldMeta.enchantmentGlintOverride)
                    }

                    when {
                        newMeta.hasMaxStackSize() -> itemMeta.setMaxStackSize(newMeta.maxStackSize)
                        oldMeta.hasMaxStackSize() -> itemMeta.setMaxStackSize(oldMeta.maxStackSize)
                    }

                    when {
                        newMeta.hasItemName() -> itemMeta.itemName(newMeta.itemName())
                        oldMeta.hasItemName() -> itemMeta.itemName(oldMeta.itemName())
                    }

                    when {
                        newMeta.hasDisplayName() -> itemMeta.displayName(newMeta.displayName())
                        oldMeta.hasDisplayName() -> itemMeta.displayName(oldMeta.displayName())
                    }
                }

                if (VersionUtil.atleast("1.21")) when {
                    newMeta.hasJukeboxPlayable() -> itemMeta.setJukeboxPlayable(newMeta.jukeboxPlayable)
                    oldMeta.hasJukeboxPlayable() -> itemMeta.setJukeboxPlayable(oldMeta.jukeboxPlayable)
                }

                if (VersionUtil.atleast("1.21.2")) {
                    when {
                        newMeta.hasEquippable() -> itemMeta.setEquippable(newMeta.equippable)
                        oldMeta.hasEquippable() -> itemMeta.setEquippable(oldMeta.equippable)
                    }

                    when {
                        newMeta.isGlider || oldMeta.isGlider -> itemMeta.isGlider = true
                    }

                    itemMeta.itemModel = newMeta.itemModel

                    when {
                        newMeta.hasUseCooldown() -> itemMeta.setUseCooldown(newMeta.useCooldown)
                        oldMeta.hasUseCooldown() -> itemMeta.setUseCooldown(oldMeta.useCooldown)
                    }

                    when {
                        newMeta.hasUseRemainder() -> itemMeta.useRemainder = newMeta.useRemainder
                        oldMeta.hasUseRemainder() -> itemMeta.useRemainder = oldMeta.useRemainder
                    }

                    when {
                        newMeta.hasDamageResistant() -> itemMeta.damageResistant = newMeta.damageResistant
                        oldMeta.hasDamageResistant() -> itemMeta.damageResistant = oldMeta.damageResistant
                    }

                    when {
                        newMeta.hasTooltipStyle() -> itemMeta.tooltipStyle = newMeta.tooltipStyle
                        oldMeta.hasTooltipStyle() -> itemMeta.tooltipStyle = oldMeta.tooltipStyle
                    }

                    when {
                        newMeta.hasEnchantable() -> itemMeta.setEnchantable(newMeta.enchantable)
                        oldMeta.hasEnchantable() -> itemMeta.setEnchantable(oldMeta.enchantable)
                    }
                }

                if (VersionUtil.atleast("1.21.4")) {
                    when {
                        newMeta.hasCustomModelData() -> itemMeta.setCustomModelDataComponent(newMeta.customModelDataComponent)
                        oldMeta.hasCustomModelData() -> itemMeta.setCustomModelDataComponent(oldMeta.customModelDataComponent)
                    }
                }

                // On 1.20.5+ we use ItemName which is different from userchanged displaynames
                // Thus removing the need for this logic
                if (VersionUtil.below("1.20.5")) {
                    val oldDisplayName = oldMeta.displayName()?.let { AdventureUtils.parseLegacy(it.serialize()) }
                    val originalName = AdventureUtils.parseLegacy(oldPdc.getOrDefault(ItemBuilder.ORIGINAL_NAME_KEY, DataType.STRING, ""))

                    itemMeta.displayName((if (originalName != oldDisplayName) oldMeta else newMeta).displayName())
                    itemPdc.set(ItemBuilder.ORIGINAL_NAME_KEY, PersistentDataType.STRING, (newMeta.displayName() ?: translatable(newItem.type)).serialize())
                }
            }

            newItem.asColorable().takeIf { oldItem.asColorable() != null }?.color = oldItem.asColorable()?.color

            val itemUtils = NMSHandlers.handler().itemUtils()
            runCatching {
                newItem.copyDataFrom(oldItem, componentsToCopy::contains)
            }.onFailure {
                itemUtils.paintingVariant(newItem, itemUtils.paintingVariant(oldItem))
            }
            NMSHandlers.handler().itemUtils().customDataComponent(newItem, newItemBuilder.customDataMap)

            return newItem
        }

        private val componentsToCopy by lazy {
            runCatching {
                listOf(
                    DataComponentTypes.DYED_COLOR,
                    DataComponentTypes.MAP_COLOR,
                    DataComponentTypes.BASE_COLOR,
                    DataComponentTypes.CONTAINER,
                    DataComponentTypes.CONTAINER_LOOT,
                    DataComponentTypes.BUNDLE_CONTENTS,
                    DataComponentTypes.PAINTING_VARIANT,
                    DataComponentTypes.WRITTEN_BOOK_CONTENT,
                    DataComponentTypes.WRITABLE_BOOK_CONTENT,
                    DataComponentTypes.CHARGED_PROJECTILES,
                    DataComponentTypes.INTANGIBLE_PROJECTILE,
                )
            }.getOrDefault(emptyList())
        }

        private fun translatable(type: Material) = Component.translatable("${if (type.isBlock) "block" else "item"}.minecraft.${type.name.lowercase()}")
    }
}
