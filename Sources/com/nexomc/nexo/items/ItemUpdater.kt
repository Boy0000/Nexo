package com.nexomc.nexo.items

import com.jeff_media.morepersistentdatatypes.DataType
import com.jeff_media.persistentdataserializer.PersistentDataSerializer
import com.mineinabyss.idofront.items.asColorable
import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.converter.OraxenConverter
import com.nexomc.nexo.nms.NMSHandlers
import com.nexomc.nexo.utils.AdventureUtils
import com.nexomc.nexo.utils.ItemUtils
import com.nexomc.nexo.utils.ItemUtils.displayName
import com.nexomc.nexo.utils.ItemUtils.editItemMeta
import com.nexomc.nexo.utils.ItemUtils.isEmpty
import com.nexomc.nexo.utils.ItemUtils.isTool
import com.nexomc.nexo.utils.ItemUtils.itemName
import com.nexomc.nexo.utils.VersionUtil
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.enchantment.PrepareItemEnchantEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.world.EntitiesLoadEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ArmorMeta
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta

@Suppress("UnstableApiUsage")
class ItemUpdater : Listener {
    @EventHandler
    fun PlayerJoinEvent.onPlayerJoin() {
        if (!Settings.UPDATE_ITEMS.toBool()) return

        for (i in 0..player.inventory.size) {
            val oldItem = player.inventory.getItem(i) ?: continue
            val newItem = updateItem(oldItem)
            if (oldItem != newItem) player.inventory.setItem(i, newItem)
        }
    }

    @EventHandler
    fun EntitiesLoadEvent.onEntitiesLoad() {
        entities.filterIsInstance<LivingEntity>().forEach { entity ->
            EquipmentSlot.entries.forEach slot@{ slot ->
                val item = runCatching { entity.equipment?.getItem(slot) }.getOrNull() ?: return@slot
                val newItem = updateItem(item)
                if (newItem != item) entity.equipment!!.setItem(slot, newItem)
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

        NexoItems.itemFromId(NexoItems.idFromItem(item))?.nexoMeta?.takeIf(NexoMeta::disableEnchanting)?.apply {
            if (result?.enchantments != item.enchantments) result = null
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun BlockBreakEvent.onUseMaxDamageItem() {
        val itemStack = player.inventory.itemInMainHand

        if (VersionUtil.below("1.20.5") || player.gameMode == GameMode.CREATIVE) return
        if (isEmpty(itemStack) || isTool(itemStack)) return
        if ((itemStack.itemMeta as? Damageable)?.hasMaxDamage() != true) return

        itemStack.takeIf { NexoItems.builderFromItem(it)?.isDamagedOnBlockBreak == true }?.damage(1, player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun EntityDamageByEntityEvent.onUseMaxDamageItem() {
        if (VersionUtil.below("1.20.5") || VersionUtil.atleast("1.21.2")) return
        val entity = damager as? LivingEntity ?: return
        val itemStack = entity.equipment?.itemInMainHand

        if (entity is Player && entity.gameMode == GameMode.CREATIVE) return
        if (isEmpty(itemStack) || isTool(itemStack!!)) return
        if ((itemStack.itemMeta as? Damageable)?.hasMaxDamage() != true) return

        itemStack.takeIf { NexoItems.builderFromItem(it)?.isDamagedOnEntityHit == true }?.damage(1, entity)
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
        else Bukkit.getScheduler().runTask(NexoPlugin.instance(), Runnable {
            for (i in 0..inventory.size) {
                val oldItem = inventory.getItem(i) ?: continue
                val newItem = updateItem(oldItem).takeIf(item::isSimilar) ?: continue

                // Remove the item and add it to fix stacking
                inventory.setItem(i, null)
                inventory.addItem(newItem)
            }
        })
    }

    companion object {
        private val IF_UUID = NamespacedKey.fromString("nexo:if-uuid")!!
        private val MF_GUI = NamespacedKey.fromString("nexo:mf-gui")!!

        fun updateItem(oldItem: ItemStack): ItemStack {

            editItemMeta(oldItem) { itemMeta: ItemMeta ->
                itemMeta.persistentDataContainer.remove(IF_UUID)
                itemMeta.persistentDataContainer.remove(MF_GUI)

                OraxenConverter.convertOraxenPDCEntries(itemMeta.persistentDataContainer)
            }

            val id = NexoItems.idFromItem(oldItem) ?: return oldItem

            val newItemBuilder = NexoItems.itemFromId(id)?.takeUnless { it.nexoMeta?.noUpdate == true } ?: return oldItem
            val newItem = NMSHandlers.handler().copyItemNBTTags(oldItem, newItemBuilder.build().clone())
            newItem.amount = oldItem.amount

            editItemMeta(newItem) { itemMeta ->
                val (oldMeta, newMeta) = (oldItem.itemMeta ?: return@editItemMeta) to (newItem.itemMeta ?: return@editItemMeta)
                val (oldPdc, itemPdc) = oldMeta.persistentDataContainer to itemMeta.persistentDataContainer

                PersistentDataSerializer.fromMapList(PersistentDataSerializer.toMapList(oldPdc), itemPdc)

                oldMeta.enchants.entries.forEach { (enchant, level) ->
                    itemMeta.addEnchant(enchant, level, true)
                }
                newMeta.enchants.entries.filterNot { oldMeta.enchants.containsKey(it.key) }.forEach { (enchant, level) ->
                    itemMeta.addEnchant(enchant, level, true)
                }

                when {
                    newMeta.hasCustomModelData() -> newMeta.customModelData
                    oldMeta.hasCustomModelData() -> oldMeta.customModelData
                    else -> null
                }.let(itemMeta::setCustomModelData)

                ItemUtils.lore(itemMeta, if (Settings.OVERRIDE_ITEM_LORE.toBool()) newMeta else oldMeta)

                if (newMeta.hasAttributeModifiers()) itemMeta.attributeModifiers = newMeta.attributeModifiers
                else if (oldMeta.hasAttributeModifiers()) itemMeta.attributeModifiers = oldMeta.attributeModifiers

                if (itemMeta is Damageable && oldMeta is Damageable && oldMeta.hasDamage()) itemMeta.damage = oldMeta.damage

                if (oldMeta.isUnbreakable) itemMeta.isUnbreakable = true

                itemMeta.asColorable().takeIf { oldMeta.asColorable() != null }?.color = oldMeta.asColorable()?.color

                if (itemMeta is ArmorMeta && oldMeta is ArmorMeta) itemMeta.trim = oldMeta.trim

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
                        newMeta.hasItemName() -> itemName(itemMeta, newMeta)
                        oldMeta.hasItemName() -> itemName(itemMeta, oldMeta)
                    }

                    when {
                        newMeta.hasDisplayName() -> displayName(itemMeta, newMeta)
                        oldMeta.hasDisplayName() -> displayName(itemMeta, oldMeta)
                    }
                }

                if (VersionUtil.atleast("1.21")) {
                    when {
                        newMeta.hasJukeboxPlayable() -> itemMeta.setJukeboxPlayable(newMeta.jukeboxPlayable)
                        oldMeta.hasJukeboxPlayable() -> itemMeta.setJukeboxPlayable(oldMeta.jukeboxPlayable)
                    }
                }

                if (VersionUtil.atleast("1.21.2")) {
                    when {
                        newMeta.hasEquippable() -> itemMeta.setEquippable(newMeta.equippable)
                        oldMeta.hasEquippable() -> itemMeta.setEquippable(oldMeta.equippable)
                    }

                    when {
                        newMeta.isGlider || oldMeta.isGlider -> itemMeta.isGlider = true
                    }

                    when {
                        newMeta.hasItemModel() -> itemMeta.itemModel = newMeta.itemModel
                        oldMeta.hasItemModel() -> itemMeta.itemModel = oldMeta.itemModel
                    }

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
                    val oldDisplayName = when {
                        oldMeta.hasDisplayName() -> AdventureUtils.parseLegacy(
                            when {
                                VersionUtil.isPaperServer -> AdventureUtils.MINI_MESSAGE.serialize(oldMeta.displayName()!!)
                                else -> AdventureUtils.parseLegacy(oldMeta.displayName)
                            }
                        )
                        else -> null
                    }
                    val originalName = AdventureUtils.parseLegacy(oldPdc.getOrDefault(ItemBuilder.ORIGINAL_NAME_KEY, DataType.STRING, ""))

                    when {
                        originalName != oldDisplayName -> displayName(itemMeta, oldMeta)
                        else -> displayName(itemMeta, newMeta)
                    }

                    itemPdc.set(
                        ItemBuilder.ORIGINAL_NAME_KEY, DataType.STRING,
                        when {
                            VersionUtil.isPaperServer -> AdventureUtils.MINI_MESSAGE.serialize(
                                newMeta.displayName() ?: translatable(newItem.type)
                            )

                            newMeta.hasDisplayName() -> newMeta.displayName
                            else -> AdventureUtils.MINI_MESSAGE.serialize(translatable(newItem.type))
                        }
                    )
                }
            }

            NMSHandlers.handler().consumableComponent(newItem, NMSHandlers.handler().consumableComponent(oldItem))
            NMSHandlers.handler().repairableComponent(newItem, NMSHandlers.handler().repairableComponent(oldItem))

            return newItem
        }

        private fun translatable(type: Material) = Component.translatable("${if (type.isBlock) "block" else "item"}.minecraft.${type.name.lowercase()}")
    }
}
