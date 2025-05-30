package com.nexomc.nexo.utils.customarmor

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.items.ItemUpdater
import com.nexomc.nexo.utils.ItemUtils
import com.nexomc.nexo.utils.logs.Logs
import net.kyori.adventure.key.Key
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.inventory.PrepareSmithingEvent
import org.bukkit.event.player.PlayerArmorStandManipulateEvent
import org.bukkit.event.player.PlayerAttemptPickupItemEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ComplexRecipe
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.inventory.meta.ArmorMeta
import org.bukkit.inventory.meta.trim.ArmorTrim
import org.bukkit.inventory.meta.trim.TrimMaterial
import org.bukkit.persistence.PersistentDataType

class CustomArmorListener : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun PrepareItemCraftEvent.onMerge() {
        if (!isRepair || recipe !is ComplexRecipe) return
        val matrix = inventory.matrix.filterNotNull()
        val itemIds = matrix.mapNotNull(NexoItems::idFromItem)

        when {
            // If no NexoItem in grid, we ignore
            itemIds.isEmpty() -> return
            // If some are NexoItems and some are not, we want to disable this
            itemIds.size != matrix.size -> inventory.result = ItemStack.empty()
            // Ensure for repair that all are the same itemid
            itemIds.distinct().size > 1 -> inventory.result = ItemStack.empty()
            // If they are all identical, ensure output is said NexoItem
            itemIds.distinct().size == 1 -> inventory.result = inventory.result?.let {
                ItemUtils.editPersistentDataContainer(it) { pdc ->
                    pdc.set(NexoItems.ITEM_ID, PersistentDataType.STRING, itemIds.first())
                }
                ItemUpdater.updateItem(it)
            }
        }
    }

    @EventHandler
    fun PlayerArmorChangeEvent.onPlayerEquipVanilla() {
        setVanillaArmorTrim(oldItem)
        setVanillaArmorTrim(newItem)
    }

    @EventHandler
    fun PlayerAttemptPickupItemEvent.onPlayerPickup() {
        this.item.itemStack = item.itemStack.apply(::setVanillaArmorTrim)
    }

    @EventHandler
    fun PrepareSmithingEvent.onTrimCustomArmor() {
        val armorMeta = inventory.inputEquipment?.takeIf { it.hasItemMeta() && NexoItems.exists(it) }?.itemMeta as? ArmorMeta ?: return
        if (CustomArmorType.setting == CustomArmorType.NONE || armorMeta.trim?.pattern?.key()?.namespace() != "nexo") return
        result = null
    }

    @EventHandler
    fun PlayerJoinEvent.updateVanillaArmor() {
        for (item in player.inventory.contents) setVanillaArmorTrim(item)
    }

    @EventHandler
    fun PlayerArmorStandManipulateEvent.onAmorStandEquip() {
        setVanillaArmorTrim(playerItem)
        setVanillaArmorTrim(armorStandItem)
    }

    private fun setVanillaArmorTrim(itemStack: ItemStack?) {
        val armorMeta = itemStack?.itemMeta as? ArmorMeta ?: return
        if (CustomArmorType.setting != CustomArmorType.TRIMS) return
        if (!itemStack.type.name.startsWith("CHAINMAIL")) return
        if (armorMeta.hasTrim() && armorMeta.trim?.pattern?.key()?.namespace() == "nexo") return

        val vanillaPatternKey = Key.key("minecraft", "chainmail")
        val vanillaPattern = Registry.TRIM_PATTERN[NamespacedKey.fromString(vanillaPatternKey.asString())!!]
        when {
            vanillaPattern != null && (!armorMeta.hasItemFlag(ItemFlag.HIDE_ARMOR_TRIM) || !armorMeta.hasTrim() || (armorMeta.trim?.pattern?.key() != vanillaPatternKey)) -> {
                armorMeta.trim = ArmorTrim(TrimMaterial.REDSTONE, vanillaPattern)
                armorMeta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM)
                itemStack.itemMeta = armorMeta
            }
            vanillaPattern == null && Settings.DEBUG.toBool() ->
                Logs.logWarn("Vanilla trim-pattern not found for " + itemStack.type.name + ": " + vanillaPatternKey.asString())
        }
    }
}
